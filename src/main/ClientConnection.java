package main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class handles the client connection to the server.
 * @author Marcus Gabilheri
 * @version 1.0
 * @since April 2014
 * 
 */
// Sorry for this code.. I did not had enough time to put it together in an object oriented way :(
// When I saw how long my class was it was almost 4 AM so.. sorry for you having to read this.
// I did created the Page class to help me handle most of the things but it was too late already.
public class ClientConnection implements Runnable{
	
	private Socket socket;
	
	public ClientConnection(Socket socket) {
		this.socket = socket;
	}
	
	public void run() {
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String headerReader = null;
			
			while ((headerReader = in.readLine()) != null) {
				//System.out.println(headerReader); // Debugging purposes
				//Break because I only want the first line of the reader which tells me the protocol and the file needed.
				if(headerReader.contains("GET")) {
					break;
				}
			}
			
			Page page = new Page(socket);
			if(headerReader != null) {
				page.retrievePage(headerReader);
			} else {
				System.out.println("Something went wrong! No response from the server!!");
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
