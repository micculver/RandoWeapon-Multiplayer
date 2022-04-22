
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RWServer {
	private ServerSocket server = null;
	
	private static Player[] players = new Player[2]; //controls player objects
	private static Thread[] threads = new Thread[2]; //controls connection to clients
	private static int[] moves = new int[2]; //keeps track of players' moves
	
	private static ReentrantLock gameLock;
	private static Condition otherPlayerConnected;
	
	private static CyclicBarrier turnBarrier = new CyclicBarrier(2);
	
	private static boolean gameOver = false;
	private static int playerCount = 0;
	
	private static ReentrantLock clientLock;
	private static Condition clientLeft;
	
	public RWServer(int port) {
		try {
			server = new ServerSocket(port);
			System.out.println("Server started");
			
			gameLock = new ReentrantLock();
			otherPlayerConnected = gameLock.newCondition();
			
			clientLock = new ReentrantLock();
			clientLeft = clientLock.newCondition();
		} 
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
			
		connect();
	}
	
	/*connect two clients before starting the game*/
	public void connect() {
		try {
			System.out.println("Waiting for clients...");
			
			while(true) {
				players[playerCount] = new Player(server.accept(), playerCount);
				threads[playerCount] = new Thread(players[playerCount]);
				threads[playerCount].start();
				playerCount++;
				
				if(playerCount == 2) {
					clientLock.lock();
					System.out.println("I'm stuck");
					clientLeft.await();
					System.out.println("I've been freed!");
					clientLock.unlock();
				}
			}
			
		}	
		catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}	
	}

	public static void executeMoves() {

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
		 * 1 -- Enemy fainted *
		 * 2 -- You fainted   *
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
			if(players[0].health == 0) {
				/*player 2 won*/
				wrToP1[2] = '2';
				wrToP2[2] = '1';
			}
			if(players[1].health == 0) {
				/*player 1 won*/
				wrToP1[2] = '1';
				wrToP2[2] = '2';
			}
		}
		System.out.println("Player 1 health: " + players[0].health);
		System.out.println("Player 2 health: " + players[1].health);
		
		
		/*in client: read in processMoves()*/
		String str1 = new String(wrToP1);
		String str2 = new String(wrToP2);
		
		System.out.println("To Player 1: " + str1);
		System.out.println("To Player 2: " + str2);
		
		try {
			players[0].dOut.writeUTF(str1);
			players[1].dOut.writeUTF(str2);
		}
		catch(IOException ioe) {
			ioe.printStackTrace();
			System.exit(1);
		}
		
		
	}


	
	private static class Player implements Runnable{
		private Socket socket;
		private DataInputStream dIn;
		private DataOutputStream dOut;
		
		private int number;
		private final int MAX_HEALTH = 1000;
		private int health;
		private Weapon weapon;
		
		private static int replayValue[] = new int[2];
		
		public Player(Socket socket, int number) {
			this.number = number;
			this.socket = socket;
			
			try {
				dIn = new DataInputStream(socket.getInputStream());
				dOut = new DataOutputStream(socket.getOutputStream());
				
				health = MAX_HEALTH;
				weapon = new Weapon();
				
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
			
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
		
		public void run() {
			
			try {
				updateValues();
				if(number == 0) {
					/*player 1 waits for player 2*/
					gameLock.lock();
					otherPlayerConnected.await();
					gameLock.unlock();
				}
				else {
					/*player 2 triggers player 1 to continue executing,
					 * leading the game to start*/
					gameLock.lock();
					otherPlayerConnected.signal();
					gameLock.unlock();
					
				}
				
				updateEnemy();
				moves[number] = -1;
				
				
				/*THE GAME RUNS THROUGH THIS LOOP*/
				while(true) {
					moves[number] = dIn.readByte();
					/*determine who makes a read first, and make them wait*/
					if(moves[Math.abs(number - 1)] == -1) {
						dOut.writeByte(0);
					}
					else {
						dOut.writeByte(1);
					}
					
					/*get both player's moves before executing moves*/
					turnBarrier.await();
					
					/*only one player needs to call the server's static method to execute the moves*/
					if(number == 0) {
						executeMoves();
					}
					
					/*finish executing moves before updating values*/
					turnBarrier.await();
					updateValues();
					updateEnemy();
					

					moves[number] = -1;
					
					/*handle JOptionPane response from client*/
					if(gameOver) {
						/*read player's response to replaying*/
						replayValue[number] = dIn.readByte();
						
						turnBarrier.await();
						
						
						if(replayValue[number] == 1) {
							/*close if player does not want to replay*/
							players[number] = null;
							dIn.close();
							socket.close();
							clientLock.lock();
							clientLeft.signal();
							clientLock.unlock();
							//turnBarrier.await();
							playerCount--;
							dOut.writeByte(0);
							dOut.close();
							
						}				
						else {
							resetValues();
							gameOver = false;
							
							Thread a = new Thread(this);
							
							if(replayValue[number] == 0 && replayValue[Math.abs(number-1)] == 1) {
								/*if only one player wants to reply*/
								if(number != 0) {
									/*if player who wants to replay is not player 1*/
									number = 0;
									players[0] = this;
									players[1] = null;
								}
	
								a.start();
								//turnBarrier.await();
							}
							else {
								/*if both players want to replay*/
								if(number == 0) {
									a.start();
									Thread b = new Thread(players[Math.abs(number - 1)]);
									Thread.sleep(1000);
									b.start();
									//turnBarrier.await();
								}
							}
							
							
						}
						
						break;
					}
				}
			} 
			catch (IOException | InterruptedException | BrokenBarrierException e) {
				
				if(e instanceof IOException)
					System.out.println("Player " + (number + 1) + " disconnected");
				else if(e instanceof InterruptedException)
					System.out.println("InterruptedException");
				else if(e instanceof BrokenBarrierException)
					System.out.println("BrokenBarrierException");
				
				e.printStackTrace();
			}
				
		}
		
		public void resetValues() {
			players[number].health = MAX_HEALTH;
			players[number].weapon.change(WeaponType.FIST);
		}
		
		/*sends new state values to the client*/
		public void updateValues() throws IOException {
			dOut.writeUTF(Integer.toString(health));
			dOut.writeUTF(weapon.toString());
			if(weapon.getUsesLeft() == -1)
				dOut.writeUTF("Infinity");
			else 
				dOut.writeUTF(Integer.toString(weapon.getUsesLeft()));
		}
		
		/*sends new enemy state values to the client*/
		public void updateEnemy() throws IOException {
			int enemy = Math.abs(number - 1);
			
			dOut.writeUTF(Integer.toString(players[enemy].health));
			dOut.writeUTF(players[enemy].weapon.toString());
		}
		
		private static class Weapon {
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
	
	public static void main(String[] args) {
		new RWServer(8000);
	}
}
