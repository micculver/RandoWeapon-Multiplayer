package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/* MATCH OBJECT 
 * Will manage an individual active game between 2 players */
public class Match {
		private Player[] players = new Player[2];
		private int[] moves = new int[2];
		private Thread[] playerThread = new Thread[2];
		private int playerCount;
		private CyclicBarrier playerBarrier;
		private boolean gameOver;
		private Server server;

		
		public Match(Server server) {
			playerCount = 0;
			playerBarrier = new CyclicBarrier(2);
			gameOver = false;
			this.server = server;
		}
		
		/* Connect a new player to this Match object */
		public void connectPlayer(Socket socket) {
			players[playerCount] = new Player(socket, playerCount, this);
			playerThread[playerCount] = new Thread(players[playerCount]);
			playerCount++;
		}
		
		/* Begin the match */
		public void startMatch() {
			players[0].setEnemy(players[1]);
			players[1].setEnemy(players[0]);
			
			playerThread[0].start();
			playerThread[1].start();
		}
		
		/* Process the moves performed by both players in each turn */
		public void executeMoves() throws IOException {

			char[] wrToP1 = new char[3];//{Player 1's move, Player 2's move, Game status} => send to player 1
			char[] wrToP2 = new char[3];//{Player 2's move, Player 1's move, Game status} => send to player 2
			
			/****************************
			 *   MOVE CODES TO CLIENT   *
			 * 0 -- successful attack   *
			 * 1 -- failed attack       *
			 * 2 -- block               *
			 * 3 -- successful dodge    *
			 * 4 -- failed dodge        *
			 * 5 -- switch weapon       *
			 ****************************/
			
			/*player 1 attacks*/
			if(moves[0] == 0) {
				int damage = players[0].attack();
				
				/*player 2 blocks*/
				if(moves[1] == 1) {
					damage /= 2;
					wrToP1[1] = '2';
					wrToP2[0] = '2';
				}
				else if(moves[1] == 2) {
					
					if(players[1].dodge()) {
						/*player 2 successful dodge*/
						damage = 0;
						wrToP1[1] = '3';
						wrToP2[0] = '3';
					}
					else {
						/*player 2 failed dodge*/
						wrToP1[1] = '4';
						wrToP2[0] = '4';
					}
				}
				
				/*player 1's attack missed*/
				if(damage == 0) {
					wrToP1[0] = '1';
					wrToP2[1] = '1';
				}
				else {
					players[1].takeDamage(damage);
					wrToP1[0] = '0';
					wrToP2[1] = '0';
				}
			}
			else if(moves[0] == 3) {
				/*player 1 switch weapon*/
				players[0].switchWeapon();
				wrToP1[0] = '5';
				wrToP2[1] = '5';
			}
			
			/*player 2 attacks*/
			if(moves[1] == 0) {
				int damage = players[1].attack();
				
				/*player 1 blocks*/
				if(moves[0] == 1) {
					damage /= 2;
					wrToP1[0] = '2';
					wrToP2[1] = '2';
				}
				else if(moves[0] == 2) {
					
					if(players[0].dodge()) {
						/*player 1 successful dodge*/
						damage = 0;
						wrToP1[0] = '3';
						wrToP2[1] = '3';
					}
					else {
						/*player 1 failed dodge*/
						wrToP1[0] = '4';
						wrToP2[1] = '4';
					}
				}
				
				/*player 2's attack missed*/
				if(damage == 0) {
					wrToP1[1] = '1';
					wrToP2[0] = '1';
				}
				else {
					players[0].takeDamage(damage);
					wrToP1[1] = '0';
					wrToP2[0] = '0';
				}
			}
			else if(moves[1] == 3) {
				/*player 2 switch weapon*/
				players[1].switchWeapon();
				wrToP1[1] = '5';
				wrToP2[0] = '5';
			}
			
			/*if both players block/dodge/switch*/
			if((moves[0] == 1 || moves[0] == 2 || moves[0] == 3) 
					&& (moves[1] == 1 || moves[1] == 2 || moves[1] == 3)) {
				if(moves[0] != 3) {
					wrToP1[0] = Integer.toString(moves[0] + 1).charAt(0);
					wrToP2[1] = Integer.toString(moves[0] + 1).charAt(0);				
				}
				else {
					wrToP1[0] = '5';
					wrToP2[1] = '5';
				}
				
				if(moves[1] != 3) {
					wrToP1[1] = Integer.toString(moves[1] + 1).charAt(0);
					wrToP2[0] = Integer.toString(moves[1] + 1).charAt(0);
				}
				else {
					wrToP1[1] = '5';
					wrToP2[0] = '5';
				}
			}
			
			
			
			/**********************
			 * GAME STATUS CODES  *
			 * 0 -- No winner yet *
			 * 1 -- You won       *
			 * 2 -- You lost      *
			 * 3 -- Tie           *
			 **********************/
			if(players[0].health != 0 && players[1].health != 0) {
				/*no winner yet*/
				wrToP1[2] = '0';
				wrToP2[2] = '0';
			}
			else {
				gameOver = true;
				
				if(players[0].health == 0 && players[1].health == 0) {
					/*tie*/
					wrToP1[2] = '3';
					wrToP2[2] = '3';
				}
				else if(players[0].health == 0) {
					/*player 2 won*/
					wrToP1[2] = '2';
					wrToP2[2] = '1';
				}
				else if(players[1].health == 0) {
					/*player 1 won*/
					wrToP1[2] = '1';
					wrToP2[2] = '2';
				}
			}
			
			/*send the clients the results of a turn*/
			String str1 = new String(wrToP1);
			String str2 = new String(wrToP2);
			players[0].dOut.writeUTF(str1);
			players[1].dOut.writeUTF(str2);

			
		}
		
		
		/* PLAYER OBJECT
		 * Will keep track of the status of one player 
		 * Responsible for interacting with the client */
		private class Player implements Runnable{
			private Socket socket;
			private DataInputStream dIn;
			private DataOutputStream dOut;
			
			private Match match;
			
			private int number; //self
			private Player enemy;
			
			private static final int MAX_HEALTH = 1000;
			private int health;
			private Weapon weapon;
			
			boolean running; //will be used for checking abrupt disconnections
			
			public Player(Socket socket, int number, Match match) {
				this.socket = socket;
				this.number = number;
				this.match = match;
				health = MAX_HEALTH;
				weapon = new Weapon();
				
				running = true;
				
				try {
					dIn = new DataInputStream(socket.getInputStream());
					dOut = new DataOutputStream(socket.getOutputStream());
				}
				catch(IOException ioe) {
					System.out.println("Failed to initialize socketstream...");
					ioe.printStackTrace();
					System.exit(1);
				}
			}
			
			public void setEnemy(Player player) {
				enemy = player;
			}
			
			@Override
			public void run() {
				try {
					updateValues();
					
					/*The duration of the game will loop here*/
					while(true) {
						match.moves[number] = -1;

						/*read what move the player has chosen*/
						match.moves[number] = dIn.readByte();
						
						/*determine who makes a read first and make them wait*/
						if(match.moves[enemy.number] == -1)
							dOut.writeByte(0);
						else
							dOut.writeByte(1);
						
						/*in case the interrupt happens outside of turnBarrier*/
						if(!players[enemy.number].running)
							throw new InterruptedException();
						
						/*both players must make a move before proceeding*/
						match.playerBarrier.await();
						
						if(number == 0)
							match.executeMoves();
						
						match.playerBarrier.await();
						updateValues();
						
						if(match.gameOver) {
							if(number == 0)
								server.closeMatch(match);

							if (dIn.readByte() == 0){
								/*if they still want to play*/
								server.addPlayer(socket);
							}
							else {
								/*player quits or an invalid value is read*/
								dIn.close();
								dOut.close();
								socket.close();
							}
							
							break;
						}
					}
				}
				catch(IOException | InterruptedException | BrokenBarrierException e) {
					if(e instanceof IOException) {
						/*abrupt disconnection*/
						running = false;
						playerThread[enemy.number].interrupt();
					}
					else if(e instanceof InterruptedException){
						try {
							/*add player to new match if opponent abruptly leaves*/
							dOut.writeUTF("z");

							server.addPlayer(socket);
						} catch (IOException ioe) {
							ioe.printStackTrace();
						}
					}	
					else if(e instanceof BrokenBarrierException) {
						System.out.println("BrokenBarrierException");
						e.printStackTrace();
					}
				}
			}
			
			
			
			/*send update of values to clients*/
			private void updateValues() throws IOException {
				dOut.writeUTF(Integer.toString(health));
				dOut.writeUTF(weapon.toString());
				if(weapon.getUsesLeft() == -1)
					dOut.writeUTF("Infinity");
				else 
					dOut.writeUTF(Integer.toString(weapon.getUsesLeft()));
				dOut.writeUTF(Integer.toString(enemy.health));
				dOut.writeUTF(enemy.weapon.toString());
			}
			
			
			
			/* Attack the other player
			 * @return damage output*/
			public int attack() {
				int damage;
				
				Random rand = new Random();
				
				if(rand.nextInt(100) < weapon.getType().getAccuracy()) 
					//hit
					damage = weapon.getType().getDamage();
				else 
					//miss
					damage = 0;
				
				/*decrement uses left if current weapon is not a fist*/
				if(weapon.getUsesLeft() != -1)
					weapon.decrement();
				
				/*switch to FIST if uses runs out*/
				if(weapon.getUsesLeft() == 0)
					weapon.change(WeaponType.FIST);
				
				return damage;
			}
			
			
			
			/* Deduct health based on damage dealt by opponent
			 * @return amount of health left*/
			public int takeDamage(int damage) {
				if(damage >= health) 
					health = 0;
				else
					health -= damage;
				
				return health;
			}
			
			
			
			/* Attempt to dodge opponent's attack
			 * @return true if successful, false otherwise*/
			public boolean dodge() {
				Random rand = new Random();
				
				/*50% chance of successful dodge*/
				if(rand.nextInt(100) < 50)
					return true;
				else
					return false;
			}
			
			
			
			/* Change this Player's weapon to a random weapon */
			public void switchWeapon() {
				Random rand = new Random();
				
				int r = rand.nextInt(100);
				
				if(r < 20)
					weapon.change(WeaponType.NUNCHUCKS);
				else if(r < 40)
					weapon.change(WeaponType.BAT);
				else if(r < 60)
					weapon.change(WeaponType.KNIFE);
				else if(r < 75) 
					weapon.change(WeaponType.SWORD);
				else if(r < 85)
					weapon.change(WeaponType.MACE);
				else if(r < 95)
					weapon.change(WeaponType.SLEDGEHAMMER);
				else
					weapon.change(WeaponType.LASERSWORD);
					
			}
			
			
			
			/* WEAPON OBJECT
			 * Keeps track of the player's current weapon*/
			private class Weapon {
				private WeaponType type;
				private int usesLeft;
				
				public Weapon() {
					type = WeaponType.FIST;
					usesLeft = type.getUses();
				}
				
				public void change(WeaponType type) {
					this.type = type;
					usesLeft = type.getUses();
				}
				
				public void decrement() {
					usesLeft--;
					
					if(usesLeft == 0) {
						change(WeaponType.FIST);
					}
				}
				
				public String toString() {
					return type.getName();
				}
				
				public int getUsesLeft() {
					return usesLeft;
				}
				
				public WeaponType getType() {
					return type;
				}
	 		}
		}
	}
