package eu.jasm.Wall2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javafx.application.Platform;

public class Communicator extends Thread {

	private App myApp;

	public Communicator(App myApp) {
		super();
		this.myApp = myApp;
	}

	@Override
	public void run() {
		super.run();
		ServerSocket serverSocket = null;;
		try {
			serverSocket = new ServerSocket(1234);
            @SuppressWarnings("unused")
			Socket clientSocket = serverSocket.accept();
		} catch (IOException e) {
			//So what
		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
				} catch (IOException e) {
					//So what
				}
			}
		}
		myApp.imageLoaderThread.interrupt();
		Platform.exit();
	}
	
	
}
