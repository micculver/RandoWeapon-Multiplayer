

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class RWClient extends JFrame implements ActionListener{
	
	private static final long serialVersionUID = -8702832296822403433L;
	private Socket socket = null;
	private DataInputStream dIn = null;
	private DataOutputStream dOut = null;
	
	private JButton attackButton;
	private JButton blockButton;
	private JButton dodgeButton;
	private JButton switchButton;
	
	private JLabel healthVal;
	private JLabel weaponVal;
	private JLabel usesVal;
	
	private JLabel oppHealthVal;
	private JLabel oppWeaponVal;
	
	private JLabel events;
	private boolean gameOver = false;
	
	public RWClient(String address, int port) {
		JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
		events = new JLabel("", SwingConstants.CENTER);
		bottomPanel.add(events);
		
		/*create and initialize buttons*/
		JPanel buttonPanel = new JPanel(new GridLayout(2, 2));
		attackButton = new JButton("Attack");
		blockButton = new JButton("Block");
		dodgeButton = new JButton("Dodge");
		switchButton = new JButton("Switch Weapon");
		attackButton.addActionListener(this);
		blockButton.addActionListener(this);
		dodgeButton.addActionListener(this);
		switchButton.addActionListener(this);
		attackButton.setEnabled(false);
		blockButton.setEnabled(false);
		dodgeButton.setEnabled(false);
		switchButton.setEnabled(false);
		buttonPanel.add(attackButton);
		buttonPanel.add(blockButton);
		buttonPanel.add(dodgeButton);
		buttonPanel.add(switchButton);
		bottomPanel.add(buttonPanel);
		this.add(bottomPanel, BorderLayout.SOUTH);
		
		
		/*create top panel*/
		JPanel topPanel = new JPanel(new GridLayout(2, 1));	
		/*create player's value panel*/
		JLabel healthLabel = new JLabel("Your Health:  ", SwingConstants.RIGHT);
		JLabel weaponLabel = new JLabel("Your Weapon:  ", SwingConstants.RIGHT);
		JLabel usesLabel = new JLabel("Uses left:  ", SwingConstants.RIGHT);
		healthVal = new JLabel();
		weaponVal = new JLabel();
		usesVal = new JLabel();
		JPanel valuesPanel = new JPanel(new GridLayout(1, 6));
		valuesPanel.add(healthLabel);
		valuesPanel.add(healthVal);
		valuesPanel.add(weaponLabel);
		valuesPanel.add(weaponVal);
		valuesPanel.add(usesLabel);
		valuesPanel.add(usesVal);
		topPanel.add(valuesPanel);
		
		/*create opponent's value panel*/
		JLabel oppHealthLabel = new JLabel("Opp. Health:  ", SwingConstants.RIGHT);
		JLabel oppWeaponLabel = new JLabel("Opp. Weapon:  ", SwingConstants.RIGHT);
		oppHealthVal = new JLabel();
		oppWeaponVal = new JLabel();
		JPanel oppPanel = new JPanel(new GridLayout(1, 6));
		oppPanel.add(oppHealthLabel);
		oppPanel.add(oppHealthVal);
		oppPanel.add(oppWeaponLabel);
		oppPanel.add(oppWeaponVal);
		oppPanel.add(new JLabel());
		oppPanel.add(new JLabel());
		
		topPanel.add(oppPanel);
		this.add(topPanel, BorderLayout.NORTH);
		
		this.setResizable(false);
		this.setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setTitle("RandoWeapon");
		this.setSize(600, 400);
		
		connectToServer(address, port);
		connectWithOpp();
	}
	
	public void connectToServer(String address, int port) {
		/*connect to the server*/
		events.setText("Connecting with ther server...");
		try {
			socket = new Socket(address, port);
			dIn = new DataInputStream(socket.getInputStream());
			dOut = new DataOutputStream(socket.getOutputStream());
			
		} catch(IOException e) {
			e.printStackTrace();
			events.setText("Could not connect to server...");
		}
	}
	
	public void connectWithOpp() {
		/*receive initial data from server*/
		updateValues();
		
		events.setText("Waiting for opponent...");
		
		/*blocks until opponent has connected*/
		updateEnemy();
		events.setText("Game started! Make a move.");
		attackButton.setEnabled(true);
		blockButton.setEnabled(true);
		dodgeButton.setEnabled(true);
		switchButton.setEnabled(true);
		System.out.println("Waiting for the player to hit a button.");
	}
	
	private void updateValues() {
		
		try {
			healthVal.setText(dIn.readUTF());
			weaponVal.setText(dIn.readUTF());
			usesVal.setText(dIn.readUTF());
		} catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	private void updateEnemy() {
		try {
			oppHealthVal.setText(dIn.readUTF());
			oppWeaponVal.setText(dIn.readUTF());
		} catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public void actionPerformed(ActionEvent e) {

		try {	
			if(e.getSource() == attackButton)
				dOut.writeByte(0);
			else if(e.getSource() == blockButton)
				dOut.writeByte(1);
			else if(e.getSource() == dodgeButton)
				dOut.writeByte(2);
			else if(e.getSource() == switchButton)
				dOut.writeByte(3);
			
			Thread t = new Thread(new Runnable() {
				public void run() {
					try {
						if(dIn.readByte() == 0) {
							events.setText("Waiting for opponent to make a move...");
							attackButton.setEnabled(false);
							blockButton.setEnabled(false);
							dodgeButton.setEnabled(false);
							switchButton.setEnabled(false);
							
							processMoves();
							
							if(!gameOver) {
								attackButton.setEnabled(true);
								blockButton.setEnabled(true);
								dodgeButton.setEnabled(true);
								switchButton.setEnabled(true);
							}
						}
						else {
							processMoves();
						}
					}
					catch(IOException ioe) {
						ioe.printStackTrace();
						System.exit(1);
					}
				}
			});
			
			t.start();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/* Determine what moves were made by each player using a code sent by the server
	 * Edit the events label and values accordingly*/
	private void processMoves() {
		try {
			String fromServer = dIn.readUTF();
			String str = "";
			
			switch(fromServer.charAt(0)) {
				case '0':
					str += "You landed a hit, ";
					break;
				case '1':
					str += "You missed, ";
					break;
				case '2':
					str += "You blocked, ";
					break;
				case '3':
					str += "You dodged, ";
					break;
				case '4':
					str += "You failed to dodge, ";
					break;
				case '5':
					str += "You switched your weapon, ";
					break;
				default:
					str += "Invalid, ";
					
			}
			
			switch(fromServer.charAt(1)) {
				case '0':
					str += "the enemy landed a hit. ";
					break;
				case '1':
					str += "the enemy missed. ";
					break;
				case '2':
					str += "the enemy blocked. ";
					break;
				case '3':
					str += "the enemy dodged. ";
					break;
				case '4':
					str += "the enemy failed to dodge. ";
					break;
				case '5':
					str += "the enemy switched their weapon. ";
					break;
				default:
					str += "Invalid. ";
				
			}
			
			updateValues();
			updateEnemy();
			
			/*if game is over*/
			if(fromServer.charAt(2) != '0') {			
				gameOver = true;
				attackButton.setEnabled(false);
				blockButton.setEnabled(false);
				dodgeButton.setEnabled(false);
				switchButton.setEnabled(false);
				
				int reply = -1;
				
				switch(fromServer.charAt(2)) {
					case '1':
						str += "The enemy fainted!";
						events.setText(str);
						events.setForeground(Color.GREEN);
						reply = JOptionPane.showConfirmDialog(null, "You won! Would you like to play again?", "Game Over", JOptionPane.YES_NO_OPTION);
						break;
					case '2':
						str += "You fainted...";
						events.setText(str);
						events.setForeground(Color.RED);
						reply = JOptionPane.showConfirmDialog(null, "You lost. Would you like to play again?", "Game Over", JOptionPane.YES_NO_OPTION);
						break;
					case '3':
						str += "You and the enemy fainted.";
						events.setText(str);
						events.setForeground(Color.BLUE);
						reply = JOptionPane.showConfirmDialog(null, "It's a tie! Would you like to play again?", "Game Over", JOptionPane.YES_NO_OPTION);
						break;
					default:
						str += "Invalid.";
				}
				
				if(reply == JOptionPane.YES_OPTION) {
					/*replay*/
					dOut.writeByte(0);
					events.setForeground(Color.BLACK);
					gameOver = false;
					connectWithOpp();
				}
				else {
					/*quit*/
					dOut.writeByte(1);
					dIn.readByte();
					this.dispose();
				}
			}
			/*if game is not over*/
			else
				events.setText(str);
			
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void main(String[] args) {
		if(args.length == 0)
			new RWClient("127.0.0.1", 8000);
		else
			new RWClient(args[0], 8000);
	}

}
































