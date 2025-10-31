package test.java.exceptionHandler;

import java.io.*;

import main.java.exceptionHandler.WarehouseException;

// This class runs test functions for all warehouse exception handling cases
public class WarehouseExceptionTest {

    public static void main(String[] args) {
        WarehouseException handler = new WarehouseException();

        System.out.println("WAREHOUSE EXCEPTION UNIT TEST \n");

        testMultipleExceptions(handler);
        testRethrowExceptions(handler);
        testResourceManagement(handler);
        testChainingExceptions(handler);
        testWarehouseOperation(handler);
    }
   
    public static void testMultipleExceptions(WarehouseException handler) {
        System.out.println("Handling Multiple Exceptions");

        System.out.println("Test 1: Missing file");
        handler.handleMultipleExceptions("File_missing.txt");

        System.out.println("\nTest 2: File exists");
        try {
            File temp = File.createTempFile("test", ".txt");
            handler.handleMultipleExceptions(temp.getAbsolutePath());
            temp.delete();
        } catch (IOException e) {
            System.out.println("Error creating test file: " + e.getMessage());
        }

        System.out.println("\nTest 3: Empty file path");
        handler.handleMultipleExceptions("");

        System.out.println("\nTest 4: Invalid file name");
        handler.handleMultipleExceptions("wrongfile.doc");

        System.out.println("\nTest 5: Null path");
        handler.handleMultipleExceptions(null);
        System.out.println();
    }

    public static void testRethrowExceptions(WarehouseException handler) {
        System.out.println("Re-throwing Exceptions");

        String[] dataTests = { null, "wares", "house", "Warehouse", "Item123" };
        for (int i = 0; i < dataTests.length; i++) {
            System.out.println("\nTest " + (i + 1) + ": Input = " + dataTests[i]);
            try {
                handler.rethrowException(dataTests[i]);
            } catch (Exception e) {
                System.out.println("Caught in test: " + e);
            }
        }
        System.out.println();
    }


    public static void testResourceManagement(WarehouseException handler) {
        System.out.println("Resource Management");

        System.out.println("\nTest 1: Missing file");
        handler.resourceManagementException("noFile.txt");


        System.out.println("\nTest 2: File with multiple lines");
        try {
            File temp = File.createTempFile("data", ".txt");
            FileWriter fw = new FileWriter(temp);
            fw.write("Line 1: Warehouse data\n");
            fw.write("Line 2: Robot activity\n");
            fw.write("Line 3: Storage log complete\n");
            fw.close();

            handler.resourceManagementException(temp.getAbsolutePath());
            temp.delete();
        } catch (IOException e) {
            System.out.println("Errorcreating test file: " + e.getMessage());
        }

        System.out.println("\nTest 3: Empty file");
        try {
            File emptyFile = File.createTempFile("empty", ".txt");
            handler.resourceManagementException(emptyFile.getAbsolutePath());
            emptyFile.delete();
        } catch (IOException e) {
            System.out.println("Error creating empty test file: " + e.getMessage());
        }

        System.out.println("\nTest 4: Wrong path");
        handler.resourceManagementException("wrongpath.txt");

        System.out.println("\nTest 5: Another missing file");
        handler.resourceManagementException("missing_file_2.txt");

        System.out.println();
    }

    public static void testChainingExceptions(WarehouseException handler) {
        System.out.println("Chaining Exceptions");
        for (int i = 1; i <= 5; i++) {
            System.out.println("\nTest " + i + ":");
            try {
                handler.chainException();
            } catch (Exception e) {
                System.out.println("Caught exception: " + e);
                System.out.println("Cause: " + e.getCause());
            }
        }
        System.out.println();
    }

    public static void testWarehouseOperation(WarehouseException handler) {
        System.out.println("General Warehouse Operation");

        System.out.println("\nTest 1: Database failure");
        handler.handleWarehouseOperation("Database", () -> {
            throw new RuntimeException("Database not reachable");
        });

        System.out.println("\nTest 2: Invalid item ID");
        handler.handleWarehouseOperation("Storage", () -> {
            throw new IllegalArgumentException("Invalid item ID");
        });

        System.out.println("\nTest 3: Robot malfunction");
        handler.handleWarehouseOperation("Robot", () -> {
            throw new NullPointerException("Robot stopped working");
        });

        System.out.println("\nTest 4: UI running fine");
        handler.handleWarehouseOperation("UI", () -> {
            System.out.println("UI started successfully");
        });

        System.out.println();
    }
}
