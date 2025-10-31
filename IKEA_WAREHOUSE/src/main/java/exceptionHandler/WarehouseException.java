package main.java.exceptionHandler;

import java.io.*;

//Handling Exceptions in IKEA Warehouse
public class WarehouseException {


 public void handleMultipleExceptions(String filePath) {
     try {
         BufferedReader br = new BufferedReader(new FileReader(filePath));
         System.out.println("File opened: " + filePath);
         br.close();
     } catch (FileNotFoundException e) {
         System.out.println("File not found: " + e.getMessage());
     } catch (IOException e) {
         System.out.println("IO exception: " + e.getMessage());
     } catch (Exception e) {
         System.out.println("IO file type: " + e.getMessage());
     }
 }


 public void rethrowException(String data) throws Exception {
     try {
         if (data == null)
             throw new NullPointerException("Null");
         if (data.length() < 3)
             throw new IllegalArgumentException("Data length");
         System.out.println("Data: " + data);
     } catch (Exception e) {
         System.out.println("Caught Exception, Re-throw");
         throw e;
     }
 }


 public void resourceManagementException(String filePath) {

	    try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

	        String line;
	        int lineCount = 0;

	        System.out.println("File: " + filePath);
	        while ((line = br.readLine()) != null) {
	            lineCount++;
	            System.out.println("Line " + lineCount + ": " + line);
	        }

	        if (lineCount == 0) {
	            System.out.println("File is empty");
	        } else {
	            System.out.println("Total lines: " + lineCount);
	        }

	    } catch (FileNotFoundException e) {
	        System.out.println("File not found: " + e.getMessage());
	    } catch (IOException e) {
	        System.out.println("IO: read exception " + e.getMessage());
	    } catch (Exception e) {
	        System.out.println("IO: handling error " + e.getMessage());
	    } finally {
	        System.out.println("Resource cleanup.");
	    }
	}


 public void chainException() throws Exception {
     try {
         throw new IOException("Disk is not working");
     } catch (IOException e) {
         throw new Exception("System startup failed", e);
     }
 }

 public void handleWarehouseOperation(String moduleName, Runnable operation) {
     try {
         operation.run();
     } catch (Exception e) {
         System.out.println("Eception Module: " + moduleName);
         System.out.println("Exception: " + e.getMessage());

     }
 }
 
 public static class InvalidTaskException extends Exception {
     /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	 public InvalidTaskException(String msg) { super(msg); }
 }

 public static class BinNotFoundException extends Exception {
     /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	 public BinNotFoundException(String msg) { super(msg); }
 }
}
