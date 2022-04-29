package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
	private Match nextMatch;
	private ServerSocket server = null;
	
	/*bind the server to the specified port and start it*/
	public void connect(int port) {
		try {
			server = new ServerSocket(port);
			System.out.println("Server is listening at port " + port);
			
			
			while(true) {
				Socket socket = server.accept();
				addPlayer(socket);
			}
		}
		catch (IOException ioe) {
			System.out.println("Failed to connect server...");
			ioe.printStackTrace();
			System.exit(1);
		}
	}
	
	/* Determine whether a new match object needs to be instantiated
	 * Connect players to a match 
	 * Start the match if it has connected its second player */
	public void addPlayer(Socket socket) {
		if(nextMatch == null) {
			/*connect first player*/
			nextMatch = new Match(this);
			nextMatch.connectPlayer(socket);
			
		}
		else {
			/*connect the second player*/
			nextMatch.connectPlayer(socket);
			nextMatch.startMatch();
			nextMatch = null;
		}
	}
	
	/* Delete a match, and make sure it isn't the current value of next match
	 * Called by match objects to delete themselves*/
	public void closeMatch(Match match) {		
		if(match == nextMatch)
			nextMatch = null;
		
		match = null;
	}
}
