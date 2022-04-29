package client;

public class ClientDriver {
	public static void main(String[] args) {
		String address;
		
		if(args.length > 0)
			address = args[0];
		else
			address = "127.0.0.1";
		
		new Window(new Client(address, 8080)).execute();
	}
}
