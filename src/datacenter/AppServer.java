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

	private static AppServer appServer;

	private static final int CLIENTS_PORT = 8887;

	private static final int DC_PORT = 8888;

	private static int nodeId;

	private AtomicInteger clock = new AtomicInteger();

	private int[][] timeTable;

	private List<Event> log;

	private List<String> messages;

	private List<String> clientIP;

	private List<String> datacenterIP;
	
	private static int nodeId;

	public AppServer() {
		this(3, 0);
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
		messages.add("Message1");
		messages.add("Message2");
		messages.add("Message3");
		messages.add("Message4");
		messages.add("Added msg!");
	}

	public AppServer(int numOfDC, int nodeId) {
		this.nodeId = nodeId;
		timeTable = new int[numOfDC][numOfDC];
		log = new ArrayList<>();
		messages = new ArrayList<>();
		clientIP = new ArrayList<>();
		datacenterIP = new ArrayList<>();
		System.out.println("My nodeId is " + this.nodeId);
	}

	private synchronized void handleClientsReq(String req, Socket socket) {
		System.out.println(req);

		String[] ss = req.split(" ", 2);
		if (ss[0].compareTo("p") == 0) {
			post(ss[1]);
		} else if (ss[0].compareTo("l") == 0) {
			lookup(socket);
		} else if (ss[0].compareTo("s") == 0) {

		}
	}

	private void post(String message) {//*
		messages.add(message);
		
		int id = nodeId;
		int currentTime = clock.getAndIncrement();
		Operation post = new PostOperation(message);
		Event e = new Event(post, currentTime, id);
		log.add(e);
		timeTable[id][id] = currentTime; 
	}

	private void lookup(Socket socket) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(messages);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sync(String desIP) {

		// System.out.println("RequestForSync!");
		// Runnable r = appServer.new SyncWithDCThread(0);
		// Thread syncWithDCThread = new Thread(r);
		// syncWithDCThread.start();
		// try {
		// syncWithDCThread.join();
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }

	}

	public boolean hasReceive(int id, int eventNode, int eventTime) {//*
		return timeTable[id][eventNode] >= eventTime;
	}
	
	private void receive(SyncData syncData) {//*
		syncData.printSyncData();
		
		//update messages 
		List<Event> tmp = syncData.getEvents();
		List<Event> newEvents = new ArrayList<>();
		int num = tmp.size();
		for(int i = 0; i < num; ++i) {
			int ti = tmp.get(i).getTime();
			int eventNode = tmp.get(i).getId();
			if(!hasReceive(nodeId, eventNode, ti)) {
				newEvents.add(tmp.get(i));
			}
		}
		
		for(int i = 0; i < newEvents.size(); ++i)
			messages.add(newEvents.get(i).getMessage());
		
		for(int i = 0; i < newEvents.size(); ++i)
			log.add(newEvents.get(i));
		
		//update timeTable
		int dim = timeTable.length;
				
		int id1 = nodeId;
		int id2 = syncData.getId();
		for(int j = 0; j < dim; ++j) {
			timeTable[id1][j] = Math.max(timeTable[id1][j], syncData.getTableEntry(id2, j));
		}
		
		for(int i = 0; i < dim; ++i) {
			for(int j = 0; j < dim; ++j) {
				timeTable[i][j] = Math.max(timeTable[i][j], syncData.getTableEntry(i, j));
			}
		}
				
		//garbage collect the log
		List<Event> tmpLog = new ArrayList<Event>();
		for(int i = 0; i < log.size(); ++i) {
			Event e = log.get(i);
			int ti = e.getTime();
			int eventNode = e.getId();
			for(int j = 0; j < dim; ++j) {
				if(!hasReceive(j, eventNode, ti)) {
					tmpLog.add(e);
					break;
				}
			}
		}
		log = tmpLog; //*
	}

	public SyncData Send(int targetNode, List<Event> logFile, int[][] TT) {
		List<Event> tmpLog = new ArrayList<Event>();
		for(int i = 0; i < logFile.size(); ++i) {
			Event e = logFile.get(i);
			int eventNode = e.getId();
			int ti = e.getTime();
			if(!hasReceive(targetNode, eventNode, ti)) {
				tmpLog.add(e);
			}
		}
		SyncData sync = new SyncData(nodeId, tmpLog, TT);
		return sync;
	}
	
	public static void main(String[] args) throws UnknownHostException {
		// int numOfDC = Integer.parseInt(args[1]);
		appServer = new AppServer();

		appServer.datacenterIP.add("127.0.0.1");

		Thread listenToClientsThread = new Thread(new ListenToClientsThread());
		listenToClientsThread.start();

		boolean requester = false;
		if (requester) {
			System.out.println("RequestForSync!");
			Runnable r = new SyncWithDCThread(0);
			Thread syncWithDCThread = new Thread(r);
			syncWithDCThread.start();
			try {
				syncWithDCThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} else {
			System.out.println("Server!");
			Thread listenToDCThread = new ListenToDCThread();
			listenToDCThread.start();
			try {
				listenToDCThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		try {
			listenToClientsThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
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

	public static class ListenToClientsThread extends Thread {
		ServerSocket listenToClientsSocket;

		@Override
		public void run() {
			try {
				listenToClientsSocket = new ServerSocket(CLIENTS_PORT, 5);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (true) {
				Socket socket;
				try {
					socket = listenToClientsSocket.accept();
					Thread t = new Thread(new ListenToClientsSocketHandler(socket));
					t.start();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public class ListenToClientsSocketHandler implements Runnable {
			private Socket connectedSocket;

			public ListenToClientsSocketHandler(Socket connectedSocket) {
				this.connectedSocket = connectedSocket;
			}

			@Override
			public void run() {
				try {
					InputStreamReader isr = new InputStreamReader(connectedSocket.getInputStream());
					BufferedReader br = new BufferedReader(isr);
					String req = br.readLine();

					AppServer.appServer.handleClientsReq(req, connectedSocket);

					connectedSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				System.out.println("End of ListenToClientsSocketHandler!");
			}
		}
	}

	public static class ListenToDCThread extends Thread {
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
<<<<<<< HEAD
					SyncData syncData = Send(targetId, AppServer.this.log, AppServer.this.timeTable);
=======
					SyncData syncData = new SyncData(AppServer.appServer.log, AppServer.appServer.timeTable);
>>>>>>> refs/remotes/origin/master
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

	public static class SyncWithDCThread implements Runnable {
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
				AppServer.appServer.receive(syncData);
				socket.close();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}
