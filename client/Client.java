package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Client {
	private Socket socket;
	private String address;
	private int port;
	
	private DataInputStream dIn;
	private DataOutputStream dOut;
	
	private boolean gameOver;

	public Client(String address, int port) {
		this.address = address;
		this.port = port;
		gameOver = false;
	}
	
	public void connectToServer() throws IOException {
		socket = new Socket(address, port);
		dIn = new DataInputStream(socket.getInputStream());
		dOut = new DataOutputStream(socket.getOutputStream());
	}
	
	public String[] readValues() throws IOException {
		String[] values = new String[5];
		
		/* [0] Player's health
		 * [1] Player's weapon
		 * [2] Player's uses left
		 * [3] Opponents's health
		 * [4] Opponent's weapon
		 * */
		for(int i = 0; i < 5; i++)
			values[i] = dIn.readUTF();
		
		return values;
	}
	
	public void writeAction(int action) throws IOException {
		dOut.writeByte(action);
	}
	
	public boolean madeFirstMove() throws IOException {
		int val = dIn.readByte();
		
		if(val == 0)
			return true;
		
		return false;
	}
	
	public String[] readTurn() throws IOException {
		String fromServer = dIn.readUTF();
		
		String[] action = new String[2];
		
		if(fromServer.equals("z")) {
			action[0] = "z";
			return action;
		}
		
		action[0] = "";
		
		switch(fromServer.charAt(0)) {
			case '0':
				action[0] += "You landed a hit, ";
				break;
			case '1':
				action[0] += "You missed, ";
				break;
			case '2':
				action[0] += "You blocked, ";
				break;
			case '3':
				action[0] += "You dodged, ";
				break;
			case '4':
				action[0] += "You failed to dodge, ";
				break;
			case '5':
				action[0] += "You switched your weapon, ";
				break;
			default:
				action[0] += "Invalid, ";
				
		}
		
		switch(fromServer.charAt(1)) {
			case '0':
				action[0] += "the enemy landed a hit. ";
				break;
			case '1':
				action[0] += "the enemy missed. ";
				break;
			case '2':
				action[0] += "the enemy blocked. ";
				break;
			case '3':
				action[0] += "the enemy dodged. ";
				break;
			case '4':
				action[0] += "the enemy failed to dodge. ";
				break;
			case '5':
				action[0] += "the enemy switched their weapon. ";
				break;
			default:
				action[0] += "Invalid. ";
		}
		
		if(fromServer.charAt(2) != '0') {
			gameOver = true;
			
			switch(fromServer.charAt(2)) {
				case '1':
					action[0] += "The enemy fainted!";
					action[1] = "You won! Play again?";
					break;
				case '2':
					action[0] += "You fainted...";
					action[1] = "You lost... Play again?";
					break;
				case '3':
					action[0] += "You and the enemy fainted.";
					action[1] = "It's a tie. Play again?";
					break;
				default:
					action[0] += "Invalid.";
			}
		}
		
		return action;
	}
	
	public boolean isGameOver() {
		return gameOver;
	}
	
	public void writeReplay(boolean wantsToReplay) throws IOException {
		if(wantsToReplay) {
			dOut.writeByte(0);
			gameOver = false;
		}
		else
			dOut.writeByte(1);
	}
	
}
