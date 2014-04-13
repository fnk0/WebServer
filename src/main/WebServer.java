package main;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * This class handles a multi threaded connection to a server.
 * When a client connects the server generates a thread and them goes back to waiting another connection.
 * @author Marcus Gabilheri
 * @since April 2014
 * @version 1.0
 *
 */
public class WebServer {

	@SuppressWarnings("resource")
	public static void main(String[] args) {
		try {
			ServerSocket serverSocket = new ServerSocket(8080);
			while (true) {
				Thread clientConnection = new Thread(new ClientConnection(serverSocket.accept()));
				clientConnection.start();
			}
		} catch (IOException e) {
			System.out.println("Server not found.");
		}
	}
}
