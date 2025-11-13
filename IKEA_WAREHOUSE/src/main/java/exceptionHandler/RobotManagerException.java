package main.java.exceptionHandler;

public class RobotManagerException extends Exception {
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
