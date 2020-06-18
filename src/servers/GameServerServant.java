package servers;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omg.CORBA.ORB;

import CoreGameServerIDL.GameServerPOA;
import exceptions.BadPasswordException;
import exceptions.BadUserNameException;
import exceptions.UnknownServerRegionException;
import models.Player;

public class GameServerServant extends GameServerPOA {
	private final ArrayList<Integer> EXT_UDP_PORTS = new ArrayList<>(Arrays.asList(6789,6790,6791));
	private int INT_UDP_PORT;
	private final int SERVER_TIMEOUT_IN_MILLIS = 5000;
	
	private HashMap<Character,ArrayList<Player>> playerHash = new HashMap<>();

	private String gameServerLocation;
	private ORB orb;

	public GameServerServant(String location) throws UnknownServerRegionException {
		super();
		this.gameServerLocation = location; 
		// create a region administrator account
		createPlayerAccount("Admin","Admin","Admin","Admin", getRegionDefaultIP(), 0);
		seedDataStore();
		setExternalPorts();
		runRegionUdpServer();
	}
	
	// CORE PLAYER FUNCTIONALITY
	
	private void seedDataStore() throws UnknownServerRegionException {
		createPlayerAccount("Allen","White","whiteallen7","password", getRegionDefaultIP(), 23);
		createPlayerAccount("Bill","Johns","billy20","password", getRegionDefaultIP(), 48);
		createPlayerAccount("Crystal","Reigo","petula71","password", getRegionDefaultIP(), 35);
	}

	public synchronized String createPlayerAccount(String fName, String lName, String uName, String password, String ipAddress, int age) {
		serverLog("Initiating CREATEACCOUNT for player", ipAddress);
		
		Character uNameFirstChar = uName.charAt(0);
		String retString = "An Error was encountered!";
		
		if(!this.playerHash.containsKey(uNameFirstChar)) {
			this.playerHash.put(uNameFirstChar, new ArrayList<Player>());
		}
		
		try {
			Player playerToAdd = new Player(fName, lName, uName, password, ipAddress, age);
			
			Optional<Player> playerExists = this.playerHash.get(uNameFirstChar)
					.stream().filter(player -> player.getuName().equals(uName)).findAny();
			
			if(playerExists.isPresent()) {
				retString = "Player with that username already exists!";
			} else {
				this.playerHash.get(uNameFirstChar).add(playerToAdd);
				retString = String.format("Successfully created account for player with username -- '%s'", uName);
			}
			
			serverLog(retString, ipAddress);
		} catch(BadUserNameException | BadPasswordException e) {
			retString = e.getMessage();
			serverLog(retString, ipAddress);
		}
		return retString; 
	}
	
	public synchronized String playerSignIn(String uName, String password, String ipAddress) {
		serverLog("Initiating SIGNIN for player", ipAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(!this.playerHash.containsKey(uNameFirstChar)) {
			String errExist = String.format("Player with username '%s' does not exist", uName);
			serverLog(errExist, ipAddress);
			return errExist;
		}
		
		Optional<Player> playerToSignIn = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
			return player.getuName().equals(uName) && player.getPassword().equals(password);
		}).findAny();
		
		if(playerToSignIn.isPresent()) {
			if(playerToSignIn.get().getStatus()) {
				String errSignedIn = String.format("Player '%s' is already signed in", uName); 
				serverLog(errSignedIn, ipAddress);
				return errSignedIn;
			} else {
				playerToSignIn.get().setStatus(true);
			}
			String success = String.format("Successfully signed in player with username -- '%s'",uName);
			serverLog(success, ipAddress);
			return success;
		}
		String errExist = String.format("Player with username '%s' and that password combination does not exist", uName);
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	public synchronized String playerSignOut(String uName, String ipAddress) {
		serverLog("Initiating SIGNOUT for player", ipAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(!this.playerHash.containsKey(uNameFirstChar)) {
			String errExist = String.format("Player with username '%s' does not exist", uName);
			serverLog(errExist, ipAddress);
			return errExist;
		}
		
		Optional<Player> playerToSignOut = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
			return player.getuName().equals(uName);
		}).findAny();
		
		if(playerToSignOut.isPresent()) {
			if(!(playerToSignOut.get().getStatus())) {
				String errSignedOut = String.format("Player '%s' is already signed out", uName);
				serverLog(errSignedOut, ipAddress);
				return errSignedOut;
			} else {
				playerToSignOut.get().setStatus(false);
			}
			String success = String.format("Successfully signed out player with username -- '%s'",uName);
			serverLog(success, ipAddress);
			return success;
		}
		
		String errExist = String.format("Player with username '%s' and that password combination does not exist", uName);
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	// END OF CORE PLAYER FUNCTIONALITY
	
	// CORE ADMIN FUNCTIONALITY
	
	public synchronized String adminSignIn(String uName, String password, String ipAddress) {
		serverLog("Initiating SIGNIN for admin", ipAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(uName.equals("Admin") && password.equals("Admin")) {
			Optional<Player> playerToSignIn = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
				return player.getuName().equals(uName) && player.getPassword().equals(password);
			}).findAny();
			
			if(playerToSignIn.isPresent()) {
				if(playerToSignIn.get().getStatus()) {
					String errSignedIn = "Admin is already signed in"; 
					serverLog(errSignedIn, ipAddress);
					return errSignedIn;
				} else {
					playerToSignIn.get().setStatus(true);
				}
				String success = "Successfully signed in admin!";
				serverLog(success, ipAddress);
				return success;
			}
		}
		
		String errExist = "Admin with that password combination does not exist";
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	public synchronized String adminSignOut(String uName, String ipAddress) {
		serverLog("Initiating SIGNOUT for admin", ipAddress);
		Character uNameFirstChar = uName.charAt(0);
		
		if(uName.equals("Admin")) {
			Optional<Player> playerToSignOut = this.playerHash.get(uNameFirstChar).stream().filter(player -> {
				return player.getuName().equals(uName);
			}).findAny();
			
			if(playerToSignOut.isPresent()) {
				if(!(playerToSignOut.get().getStatus())) {
					String errSignedOut = "Admin is already signed out";
					serverLog(errSignedOut, ipAddress);
					return errSignedOut;
				} else {
					playerToSignOut.get().setStatus(false);
				}
				String success = "Successfully signed out admin";
				serverLog(success, ipAddress);
				return success;
			}
		}
		
		String errExist = "Admin with that password combination does not exist";
		serverLog(errExist, ipAddress);
		return errExist;
	}
	
	public String getPlayerStatus(String uName, String password, String ipAddress) {
		String retStatement = "Unrecognized Error while requesting player status!";
		
		if(!(uName.equals("Admin") && password.equals("Admin"))) {
			retStatement = "Incorrect credentials for Admin!";
		} else {
		
			Optional<Player> admin = this.playerHash.get('A').stream().filter(player -> {
				return player.getuName().equals("Admin") && 
						player.getPassword().equals("Admin");
			}).findAny();
				
			if(admin.isPresent()) {
					String ret = retrievePlayerStatuses(ipAddress);
					serverLog(ret, ipAddress);
					return ret;
			}
		}
		
		serverLog(retStatement, ipAddress);
		return retStatement;
	}
	
	// END OF CORE ADMIN FUNCTIONALITY
	
	// UTILITIES AND HELPERS
	
	private String retrievePlayerStatuses(String ipAddress) {
	    CompletableFuture<String> intRetrieve = CompletableFuture.supplyAsync(()->{
			return getPlayerCounts();
	    });

	    CompletableFuture<String> extRetrieve1 = CompletableFuture.supplyAsync(()->{
	    	return makeUDPRequestToExternalServer(EXT_UDP_PORTS.get(0));
	    });

	    CompletableFuture<String> extRetrieve2 = CompletableFuture.supplyAsync(()->{
	    	return makeUDPRequestToExternalServer(EXT_UDP_PORTS.get(1));
	    });

	    CompletableFuture<Void> allRetrieve = CompletableFuture.allOf(intRetrieve, extRetrieve1, extRetrieve2); 
	    
	    try {
	        allRetrieve.get();
	        String retSucc = Stream.of(intRetrieve, extRetrieve1, extRetrieve2)
	        		.map(CompletableFuture::join)
	        		.collect(Collectors.joining("\n"));
	        serverLog(retSucc,ipAddress);
	        return retSucc;
	    } catch (Exception e) {
	    	e.printStackTrace();
		    String err = e.getMessage();
		    serverLog(err, ipAddress);
	    }
	    String err = "ERROR Could not retrieve player statuses!";
	    serverLog(err, ipAddress);
	    return err;
	}
	
	private String getPlayerCounts() {
		int online = 0;
		int offline = 0;
		for(Character index : this.playerHash.keySet()) {
			for(Player player : this.playerHash.get(index)) {
				if(player.getfName().equals("Admin")) continue;
				if(player.getStatus()) {
					online += 1;
				} else {
					offline += 1;
				}
			}
		}
		String succ = String.format("%s: Online: %d Offline: %d", this.gameServerLocation, online, offline);
		serverLog(succ, "Admin@"+this.gameServerLocation);
		return succ;
	}
	
	// NETWORK UTILS 
	
	private void runRegionUdpServer() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		
    	executorService.execute((Runnable) ()->{
    	  String log = String.format("Starting UDP Server for %s region on port %d ...",gameServerLocation, INT_UDP_PORT);
		  System.out.println(log);
		  serverLog("Admin",log);
		  listenForPlayerStatusRequests();
		});
		
	}
	
	private void listenForPlayerStatusRequests() {
		// UDP server awaiting requests from other game servers
		DatagramSocket aSocket = null;
		try{
	    	aSocket = new DatagramSocket(INT_UDP_PORT);
			byte[] buffer = new byte[1000];
 			while(true){
 				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
  				aSocket.receive(request);     
  				String toSend = this.getPlayerCounts();
    			DatagramPacket reply = new DatagramPacket(toSend.getBytes(), toSend.getBytes().length, request.getAddress(), request.getPort());
    			aSocket.send(reply);
    		}
		} catch (SocketException e){
			System.out.println("Socket Exception: " + e.getMessage());
			serverLog(e.getMessage(), "Admin");
		} catch (IOException e) {
			System.out.println("IO Exception: " + e.getMessage());
			serverLog(e.getMessage(), "Admin");
		} finally {
			if(aSocket != null) aSocket.close();
		}
	}
	
	private String makeUDPRequestToExternalServer(int serverPort) {
		DatagramSocket aSocket = null;
		String reqOp = "getStatus";
		try {
			aSocket = new DatagramSocket();    
			aSocket.setSoTimeout(SERVER_TIMEOUT_IN_MILLIS); 
			byte [] m = reqOp.getBytes();
			InetAddress aHost = InetAddress.getByName("127.0.0.1");		                                                 
			DatagramPacket request =
			 	new DatagramPacket(m, reqOp.length(), aHost, serverPort);
			aSocket.send(request);			                        
			byte[] buffer = new byte[1000];
			DatagramPacket reply = new DatagramPacket(buffer, buffer.length);	
			aSocket.receive(reply);
			String succ = new String(reply.getData());	
			serverLog(succ, "Admin");
			return succ;
		} catch (SocketTimeoutException e) {
			String timeOut = String.format("Request to server on port %d has timed out!", serverPort);
			serverLog(timeOut, "Admin");
			return timeOut;
		} catch (SocketException e){
			serverLog(e.getMessage(), "Admin");
			return "Socket Exception: " + e.getMessage();
		} catch (IOException e) {
			serverLog(e.getMessage(), "Admin");
			return "IO Exception: " + e.getMessage();
		} finally {
			if(aSocket != null) aSocket.close();
		}
	}
	
	// END OF NETWORK UTILS
	
	private void setExternalPorts() throws UnknownServerRegionException {
		switch(this.gameServerLocation) {
			case "NA": {
				INT_UDP_PORT = Integer.valueOf(EXT_UDP_PORTS.get(0));
				EXT_UDP_PORTS.remove(0);
				break;
			}
			case "EU": {
				INT_UDP_PORT = Integer.valueOf(EXT_UDP_PORTS.get(1));
				EXT_UDP_PORTS.remove(1);
				break;
			}
			case "AS": {
				INT_UDP_PORT = Integer.valueOf(EXT_UDP_PORTS.get(2));
				EXT_UDP_PORTS.remove(2);
				break;
			}
			default:
				throw new UnknownServerRegionException();
		}
	}

	private String getRegionDefaultIP() throws UnknownServerRegionException {
		switch(gameServerLocation) {
			case "NA":
				return "132.168.2.22";
			case "EU":
				return "93.168.2.22";
			case "AS":
				return "182.168.2.22";
			default:
				throw new UnknownServerRegionException();
		}
	}

	private synchronized void serverLog(String logStatement, String ipAddress) {
		 DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
		 LocalDateTime tStamp = LocalDateTime.now(); 
		 String writeString = String.format("[%s] Response to %s -- %s", dtf.format(tStamp), ipAddress, logStatement);
		 try{
			File file = new File(String.format("server_logs/%s-server.log", this.gameServerLocation));
			file.getParentFile().mkdirs();
			FileWriter fw = new FileWriter(file, true);
			BufferedWriter logger = new BufferedWriter(fw);
			logger.write(writeString);
			logger.newLine();
			logger.close();
		} catch (IOException e) {
			// can't really log an error while logging
			e.printStackTrace();
		}
	}

	public void setORB(ORB orb) {
		this.orb = orb; 
	}
	
	public void shutdown() {
		orb.shutdown(false);
	}
}
