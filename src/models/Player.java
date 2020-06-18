package models;

import exceptions.BadPasswordException;
import exceptions.BadUserNameException;

public class Player {
	private String fName, lName, uName, password, ipAddress;
	private boolean status;
	private int age;
	
	public Player(String fName, String lName, String uName, String password, String ipAddress, int age) throws BadUserNameException, BadPasswordException {
		this.setfName(fName);
		this.setlName(lName);
		this.setuName(uName);
		this.setPassword(password);
		this.setIpAddress(ipAddress);
		this.setAge(age);
		this.setStatus(false);
	}
	
	public String getfName() {
		return fName;
	}
	public void setfName(String fName) {
		this.fName = fName;
	}
	public String getlName() {
		return lName;
	}
	public void setlName(String lName) {
		this.lName = lName;
	}
	public String getuName() {
		return uName;
	}
	public void setuName(String uName) throws BadUserNameException {
		if(uName.equals("Admin") || (uName.length() >= 6 && uName.length() <=15)) {
			this.uName = uName;
		} else {
			throw new BadUserNameException();
		}
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String passWord) throws BadPasswordException {
		if(passWord.equals("Admin") || (passWord.length() >= 6)) {
			this.password = passWord;
		} else {
			throw new BadPasswordException();
		}
	}
	public String getIpAddress() {
		return ipAddress;
	}
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
	public boolean getStatus() {
		return status;
	}
	public void setStatus(boolean status) {
		this.status = status;
	}
}
