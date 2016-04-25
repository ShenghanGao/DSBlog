package datacenter;

import java.io.Serializable;

import api.Operation;

public class Event implements Serializable {
	private Operation op;
	private int time;
	private int nodeId;

	public Event(Operation op, int time, int nodeId) {
		this.op = op;
		this.time = time;
		this.nodeId = nodeId;
	}
	
	public int getTime(){
		return time;
	}
	
	public int getId(){
		return nodeId;
	}
	
	public String getMessage(){
		return op.getMessage();
	}
}
