package api;

import java.io.Serializable;

public abstract class Operation implements Serializable {
	private OperationType operationType;

	public Operation(OperationType operationType) {
		this.operationType = operationType;
	}

	public OperationType getOperationType() {
		return this.operationType;
	}

	public abstract String getOperationParameters();
}
