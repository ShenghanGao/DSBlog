package datacenter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SyncData implements Serializable {
	private static final long serialVersionUID = 1965304440603696756L;
	private List<Event> events;
	private int[][] timeTable;
	private int nodeId;
	
	public SyncData(List<Event> e, int[][] tt) {
		int dim = tt.length;
		events = new ArrayList<>(e);
		timeTable = new int[dim][dim];

		System.out.println(tt.length);
		for (int i = 0; i < dim; ++i) {
			for (int j = 0; j < dim; ++j) {
				timeTable[i][j] = tt[i][j];
			}
		}
	}

	public SyncData(int id, List<Event> e, int[][] tt) {
		nodeId = id;
		int dim = tt.length;
		events = new ArrayList<>(e);
		timeTable = new int[dim][dim];

		System.out.println(tt.length);
		for (int i = 0; i < dim; ++i) {
			for (int j = 0; j < dim; ++j) {
				timeTable[i][j] = tt[i][j];
			}
		}
	}
	
	public List<Event> getEvents() {
		return events;
	}

	public int getTableEntry(int x, int y) {
		return timeTable[x][y];
	}

	public void setTableEntry(int x, int y, int val) {
		timeTable[x][y] = val;
	}

	public int getId(){
		return nodeId;
	}
	
	public void printSyncData() {
		int dim = timeTable.length;

		for (int i = 0; i < dim; ++i) {
			for (int j = 0; j < dim; ++j) {
				System.out.print(timeTable[i][j] + " ");
			}
			System.out.println();
		}
	}
}
