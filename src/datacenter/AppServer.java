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

	private List<String> datacenterIP;

	public AppServer(int numOfDC, List<String> datacenterIPs) {
		this.timeTable = new int[numOfDC][numOfDC];
		this.log = new ArrayList<>();
		this.messages = new ArrayList<>();
		this.datacenterIP = new ArrayList<>(datacenterIPs);
	}

	private synchronized void handleClientsReq(String req, Socket socket) {
		String[] ss = req.split(" ", 2);
		if (ss[0].compareTo("p") == 0) {
			post(ss[1]);
		} else if (ss[0].compareTo("l") == 0) {
			lookup(socket);
		} else if (ss[0].compareTo("s") == 0) {
			sync(Integer.parseInt(ss[1]));
		}
	}

	private void post(String message) {
		messages.add(message);

		int id = AppServer.nodeId;
		int currentTime = clock.incrementAndGet();
		Operation post = new PostOperation(message);
		Event e = new Event(post, currentTime, id);
		log.add(e);
		timeTable[id - 1][id - 1] = currentTime;
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

	private void sync(int des) {
		System.out.println("RequestForSync!");
		Runnable r = new SyncWithDCThread(des);
		Thread syncWithDCThread = new Thread(r);
		syncWithDCThread.start();
		try {
			syncWithDCThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static boolean hasReceive(int[][] timeTable, int id, int eventNode, int eventTime) {
		return timeTable[id - 1][eventNode - 1] >= eventTime;
	}

	private void receive(SyncData syncData) {
		syncData.printSyncData();

		// update messages
		List<Event> tmp = syncData.getEvents();
		List<Event> newEvents = new ArrayList<>();
		int num = tmp.size();
		for (int i = 0; i < num; ++i) {
			int eventNode = tmp.get(i).getNodeId();
			int ti = tmp.get(i).getTime();
			if (!hasReceive(this.timeTable, nodeId, eventNode, ti)) {
				newEvents.add(tmp.get(i));
			}
		}

		for (int i = 0; i < newEvents.size(); ++i)
			messages.add(newEvents.get(i).getOperationParameters());

		for (int i = 0; i < newEvents.size(); ++i)
			log.add(newEvents.get(i));

		// update timeTable
		int dim = timeTable.length;

		int id1 = nodeId;
		int id2 = syncData.getNodeId();
		for (int j = 0; j < dim; ++j) {
			timeTable[id1 - 1][j] = Math.max(timeTable[id1 - 1][j], syncData.getTableEntry(id2 - 1, j));
		}

		for (int i = 0; i < dim; ++i) {
			for (int j = 0; j < dim; ++j) {
				timeTable[i][j] = Math.max(timeTable[i][j], syncData.getTableEntry(i, j));
			}
		}

		// garbage collect the log
		List<Event> tmpLog = new ArrayList<>();
		for (int i = 0; i < log.size(); ++i) {
			Event e = log.get(i);
			int eventNode = e.getNodeId();
			int ti = e.getTime();
			for (int j = 1; j <= dim; ++j) {
				if (!hasReceive(this.timeTable, j, eventNode, ti)) {
					tmpLog.add(e);
					break;
				}
			}
		}
		log = tmpLog;
	}

	public static void main(String[] args) throws UnknownHostException {
		List<String> datacenterIPs = new ArrayList<>();

		datacenterIPs.add("128.111.43.40"); // thundarr.cs.ucsb.edu
		datacenterIPs.add("128.111.43.41"); //optimus.cs.ucsb.edu
		datacenterIPs.add("128.111.43.42"); //megatron.cs.ucsb.edu
		//datacenterIPs.add("128.111.84.167");
		//datacenterIPs.add("128.111.84.203");
		//datacenterIPs.add("128.111.43.56");

		int numOfNodes = datacenterIPs.size();

		InetAddress inetAddress = InetAddress.getLocalHost();
		String myIPAddress = inetAddress.getHostAddress();

		// System.out.println("IPAddress = " + IPAddress);

		AppServer.appServer = new AppServer(numOfNodes, datacenterIPs);

		int k = 0;
		for (; k < numOfNodes; ++k) {
			if (myIPAddress.compareTo(appServer.datacenterIP.get(k)) == 0)
				break;
		}
		if (k == numOfNodes) {
			System.out.println("The IP address of this machine is not in the list!");
			return;
		}
		AppServer.nodeId = k + 1;
		System.out.println("My nodeId is " + AppServer.nodeId);
		System.out.println("TT dim is " + AppServer.appServer.timeTable.length);

		Thread listenToClientsThread = new Thread(new ListenToClientsThread());
		listenToClientsThread.start();

		System.out.println("Server!");
		Thread listenToDCThread = new ListenToDCThread();
		listenToDCThread.start();
		try {
			listenToDCThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

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
				// System.out.println("End of ListenToClientsSocketHandler!");
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
					int targetId = Integer.parseInt(br.readLine());
					// System.out.println("signal = " + signal);

					if (signal.compareTo("S") != 0)
						return;

					System.out.println("targetId = " + targetId);

					ObjectOutputStream oos = new ObjectOutputStream(connectedSocket.getOutputStream());

					SyncData syncData = send(targetId, AppServer.appServer.log, AppServer.appServer.timeTable);

					oos.writeObject(syncData);
					oos.flush();
					connectedSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				System.out.println("End of ListenToDCSocketHandler!");
			}

			public SyncData send(int targetNode, List<Event> logFile, int[][] TT) {
				List<Event> tmpLog = new ArrayList<>();
				for (int i = 0; i < logFile.size(); ++i) {
					Event e = logFile.get(i);
					int eventNode = e.getNodeId();
					int ti = e.getTime();
					if (!AppServer.hasReceive(TT, targetNode, eventNode, ti)) {
						tmpLog.add(e);
					}
				}
				SyncData sync = new SyncData(nodeId, tmpLog, TT);
				return sync;
			}
		}
	}

	public static class SyncWithDCThread implements Runnable {
		Socket socket;

		private InetAddress desAddress;

		public SyncWithDCThread(int des) {
			try {
				desAddress = InetAddress.getByName(AppServer.appServer.datacenterIP.get(des - 1));
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
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
			pw.println(AppServer.nodeId);
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
