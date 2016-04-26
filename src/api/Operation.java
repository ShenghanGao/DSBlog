package api;

import java.io.Serializable;

public abstract class Operation implements Serializable {
	private static final long serialVersionUID = -6273180531795936449L;
	private OperationType operationType;

	public Operation(OperationType operationType) {
		this.operationType = operationType;
	}

	public OperationType getOperationType() {
		return this.operationType;
	}

	public abstract String getOperationParameters();
}
