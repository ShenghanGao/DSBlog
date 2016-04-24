package datacenter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import api.Operation;
import api.PostOperation;

public class AppServer {

	// private static AppServer appServer;

	private static final int DC_PORT = 8888;

	private AtomicInteger clock = new AtomicInteger();

	private int[][] timeTable;

	private List<Event> log;

	private List<String> messages;

	private List<String> clientIP;

	private List<String> datacenterIP;

	public AppServer() {
		this(3);
		int cnt = 2;
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 3; ++j) {
				this.timeTable[i][j] = cnt++;
			}
		}
		Operation post = new PostOperation("Message 1");
		Event e = new Event(post, 1, 1);
		log.add(e);
		post = new PostOperation("Message 2");
		e = new Event(post, 2, 2);
		log.add(e);
	}

	public AppServer(int numOfDC) {
		timeTable = new int[numOfDC][numOfDC];
		log = new ArrayList<>();
		messages = new ArrayList<>();
		clientIP = new ArrayList<>();
		datacenterIP = new ArrayList<>();
	}

	private void receive(SyncData syncData) {
		syncData.printSyncData();
	}

	public static void main(String[] args) throws UnknownHostException {
		// int numOfDC = Integer.parseInt(args[1]);
		AppServer appServer = new AppServer();
		appServer.datacenterIP.add("127.0.0.1");

		boolean requester = false;
		if (requester) {
			System.out.println("RequestForSync!");
			Runnable r = appServer.new SyncWithDCThread(0);
			Thread syncWithDCThread = new Thread(r);
			syncWithDCThread.start();
			try {
				syncWithDCThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} else {
			System.out.println("Server!");
			Thread listenToDCThread = appServer.new ListenToDCThread();
			listenToDCThread.start();
			try {
				listenToDCThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * private class ListenToDCThread extends Thread { ServerSocket
	 * listenToDCSocket;
	 * 
	 * @Override public void run() { try { listenToDCSocket = new
	 * ServerSocket(DC_PORT, 5); } catch (IOException e) { e.printStackTrace();
	 * } while (true) { Socket socket; try { socket = listenToDCSocket.accept();
	 * ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
	 * SyncData syncData = (SyncData) ois.readObject();
	 * AppServer.this.receive(syncData); socket.close(); } catch (IOException |
	 * ClassNotFoundException e) { e.printStackTrace(); } } } }
	 */

	/*
	 * public class SendToDCThread implements Runnable { Socket socket;
	 * 
	 * private InetAddress desAddress;
	 * 
	 * public SendToDCThread(int des) throws UnknownHostException { //
	 * desAddress = //
	 * InetAddress.getByName(AppServer.this.datacenterIP.get(des)); desAddress =
	 * InetAddress.getByName("127.0.0.1"); }
	 * 
	 * @Override public void run() { try { socket = new Socket(desAddress,
	 * DC_PORT); } catch (IOException e) { e.printStackTrace(); }
	 * 
	 * try { ObjectOutputStream oos = new
	 * ObjectOutputStream(socket.getOutputStream()); SyncData syncData = new
	 * SyncData(AppServer.this.log, AppServer.this.timeTable);
	 * oos.writeObject(syncData); oos.flush(); socket.close(); } catch
	 * (IOException e) { e.printStackTrace(); } } }
	 */

	public class ListenToDCThread extends Thread {
		ServerSocket listenToDCSocket;

		@Override
		public void run() {
			try {
				listenToDCSocket = new ServerSocket(DC_PORT, 5);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (true) {
				Socket socket;
				try {
					socket = listenToDCSocket.accept();
					Thread t = new Thread(new ListenToDCSocketHandler(socket));
					t.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public class ListenToDCSocketHandler implements Runnable {
			private Socket connectedSocket;

			public ListenToDCSocketHandler(Socket connectedSocket) {
				this.connectedSocket = connectedSocket;
			}

			@Override
			public void run() {
				try {
					InputStreamReader isr = new InputStreamReader(connectedSocket.getInputStream());
					BufferedReader br = new BufferedReader(isr);
					String signal = br.readLine();
					System.out.println("signal = " + signal);

					ObjectOutputStream oos = new ObjectOutputStream(connectedSocket.getOutputStream());
					SyncData syncData = new SyncData(AppServer.this.log, AppServer.this.timeTable);
					oos.writeObject(syncData);
					oos.flush();
					connectedSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("End of ListenToDCSocketHandler!");
			}
		}
	}

	public class SyncWithDCThread implements Runnable {
		Socket socket;

		private InetAddress desAddress;

		public SyncWithDCThread(int des) throws UnknownHostException {
			// desAddress =
			// InetAddress.getByName(AppServer.this.datacenterIP.get(des));
			desAddress = InetAddress.getByName("127.0.0.1");
		}

		@Override
		public void run() {
			try {
				socket = new Socket(desAddress, DC_PORT);
			} catch (IOException e) {
				e.printStackTrace();
			}
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(socket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
			pw.println("S");
			pw.flush();

			try {
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				SyncData syncData = (SyncData) ois.readObject();
				AppServer.this.receive(syncData);
				socket.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
