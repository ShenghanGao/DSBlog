package api;

public class PostOperation extends Operation {
	private String message;

	public PostOperation(String message) {
		super(OperationType.POST);
		this.message = message;
	}

	@Override
	public String getOperationParameters() {
		return message;
	}
}
