package client;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class Window extends JFrame implements ActionListener{
	
	private static final long serialVersionUID = -8702832296822403433L;
	
	private Client client;
	
	private JButton attackButton;
	private JButton blockButton;
	private JButton dodgeButton;
	private JButton switchButton;
	
	private JLabel healthVal;
	private JLabel weaponVal;
	private JLabel usesVal;
	
	private JLabel oppHealthVal;
	private JLabel oppWeaponVal;
	
	private JLabel actionsLabel;
	
	public Window(Client client) {
		JPanel bottomPanel = new JPanel(new GridLayout(2, 1));
		actionsLabel = new JLabel("", SwingConstants.CENTER);
		bottomPanel.add(actionsLabel);
		
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
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("RandoWeapon");
		this.setSize(600, 400);
		
		this.client = client;
	}
	
	/*Attempt to bind socket to server socket*/
	public void execute() {
		try {
			actionsLabel.setText("Connecting with the server...");
			client.connectToServer();
			connectWithOpp();
		} catch(IOException e) {
			e.printStackTrace();
			actionsLabel.setText("Could not connect to server...");
		}
	}
	
	/* Connect with opponent
	 * Make the UI playable once achieved */
	public void connectWithOpp() {
		actionsLabel.setText("Waiting for opponent...");
		
		updateValues();
		
		actionsLabel.setText("Game started! Make a move.");
		attackButton.setEnabled(true);
		blockButton.setEnabled(true);
		dodgeButton.setEnabled(true);
		switchButton.setEnabled(true);
	}
	
	public void updateValues() {
		try {
			String[] values = client.readValues();
			
			healthVal.setText(values[0]);
			weaponVal.setText(values[1]);
			usesVal.setText(values[2]);
			oppHealthVal.setText(values[3]);
			oppWeaponVal.setText(values[4]);
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void actionPerformed(ActionEvent e) {

		try {	
			if(e.getSource() == attackButton)
				client.writeAction(0);
			else if(e.getSource() == blockButton)
				client.writeAction(1);
			else if(e.getSource() == dodgeButton)
				client.writeAction(2);
			else if(e.getSource() == switchButton)
				client.writeAction(3);
			
			/*Wait for opponent to make a move if they have not made a move*/
			Thread t = new Thread(new Runnable() {
				public void run() {
					try {
						if(client.madeFirstMove()) {
							actionsLabel.setText("Waiting for opponent to make a move...");
							attackButton.setEnabled(false);
							blockButton.setEnabled(false);
							dodgeButton.setEnabled(false);
							switchButton.setEnabled(false);
							
							updateGame();
							
							if(!client.isGameOver()) {
								attackButton.setEnabled(true);
								blockButton.setEnabled(true);
								dodgeButton.setEnabled(true);
								switchButton.setEnabled(true);
							}
						}
						else {
							updateGame();
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
	private void updateGame() {
		try {
			String[] action = client.readTurn();
			
			if(action[0].equals("z"))
				connectWithOpp();
			
			else {
				updateValues();
				
				actionsLabel.setText(action[0]);
				/*if game is over*/
				if(client.isGameOver()) {			
					attackButton.setEnabled(false);
					blockButton.setEnabled(false);
					dodgeButton.setEnabled(false);
					switchButton.setEnabled(false);
					
					int reply = JOptionPane.showConfirmDialog(null,
							action[1], "Game Over", JOptionPane.YES_NO_OPTION);
					
					if(reply == JOptionPane.YES_OPTION) {
						/*replay*/
						client.writeReplay(true);
						connectWithOpp();
					}
					else if (reply == JOptionPane.NO_OPTION) {
						/*quit*/
						client.writeReplay(false);
						this.dispose();
					}
					else {
						System.out.println("Invalid: " + reply);
						System.exit(1);
					}
				}
			}
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}
