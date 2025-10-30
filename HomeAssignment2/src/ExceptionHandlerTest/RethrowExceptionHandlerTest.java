package ExceptionHandlerTest;

import ExceptionHandler.RethrowExceptionHandler;
import ExceptionHandler.RethrowExceptionHandler.WarehouseException;

public class RethrowExceptionHandlerTest {

    public static void main(String[] args) {
        RethrowExceptionHandlerTest test = new RethrowExceptionHandlerTest();
        test.testValidQuantity();
        test.testInvalidQuantityString();
        test.testEmptyQuantityString();
        test.testNullQuantityString();
        test.testFileNotFoundThrowsWarehouseException();
    }

    public void testValidQuantity() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        int result = h.parseQuantity("25");
        System.out.println("testValidQuantity: " + (result == 25 ? "PASS" : "FAIL"));
    }

    public void testInvalidQuantityString() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        try {
            h.parseQuantity("abc");
            System.out.println("testInvalidQuantityString: FAIL (no exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("testInvalidQuantityString: PASS");
        }
    }

    public void testEmptyQuantityString() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        try {
            h.parseQuantity("   ");
            System.out.println("testEmptyQuantityString: FAIL (no exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("testEmptyQuantityString: PASS");
        }
    }

    public void testNullQuantityString() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        try {
            h.parseQuantity(null);
            System.out.println("testNullQuantityString: FAIL (no exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("testNullQuantityString: PASS");
        }
    }

    public void testFileNotFoundThrowsWarehouseException() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        try {
            h.getFileSize("doesnotexist.txt");
            System.out.println("testFileNotFoundThrowsWarehouseException: FAIL (no exception)");
        } catch (WarehouseException e) {
            boolean valid = e.getMessage().contains("Error while reading file") && e.getCause() != null;
            System.out.println("testFileNotFoundThrowsWarehouseException: " + (valid ? "PASS" : "FAIL"));
        }
    }
}
