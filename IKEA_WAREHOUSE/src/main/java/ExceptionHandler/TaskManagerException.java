package main.java.ExceptionHandler;

public class TaskManagerException extends RuntimeException {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TaskManagerException(String message) {
        super(message);
    }

    public TaskManagerException(String message, Throwable cause) {
        super(message, cause);
    }
}
