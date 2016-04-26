package datacenter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SyncData implements Serializable {
	private static final long serialVersionUID = 1965304440603696756L;

	private int nodeId;
	private List<Event> events;
	private int[][] timeTable;

	public SyncData(int nodeId, List<Event> e, int[][] tt) {
		this.nodeId = nodeId;
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

	public int getNodeId() {
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
