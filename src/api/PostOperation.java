package api;

public class PostOperation extends Operation {
	private static final long serialVersionUID = -4037561128557297111L;
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
