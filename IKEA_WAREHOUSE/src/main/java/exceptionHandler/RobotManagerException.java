package main.java.exceptionHandler;

public class RobotManagerException extends Exception {
	private static final long serialVersionUID = 1L;

	public RobotManagerException(String message) {
		super(message);
	}

	public RobotManagerException(Throwable cause) {
		super(cause);
	}

	public RobotManagerException(String message, Throwable cause) {
		super(message, cause);
	}
}
