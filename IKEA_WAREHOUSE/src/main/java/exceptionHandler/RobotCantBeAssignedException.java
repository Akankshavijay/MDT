package main.java.exceptionHandler;

public class RobotCantBeAssignedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public RobotCantBeAssignedException(String message) {
		super(message);
	}

	public RobotCantBeAssignedException(Throwable cause) {
		super(cause);
	}

	public RobotCantBeAssignedException(String message, Throwable cause) {
		super(message, cause);
	}
}