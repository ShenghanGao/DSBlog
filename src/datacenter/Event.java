package datacenter;

import java.io.Serializable;

import api.Operation;

public class Event implements Serializable {

	private static final long serialVersionUID = 3515128752435636560L;
	private Operation op;
	private int time;
	private int nodeId;

	public Event(Operation op, int time, int nodeId) {
		this.op = op;
		this.time = time;
		this.nodeId = nodeId;
	}

	public int getTime() {
		return time;
	}

	public int getNodeId() {
		return nodeId;
	}

	public String getOperationParameters() {
		return op.getOperationParameters();
	}
}
