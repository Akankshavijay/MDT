package Exception_Handler.tests;

import Exception_Handler.RethrowExceptionHandler;
import org.junit.Test;
import static org.junit.Assert.*;

public class RethrowExceptionHandlerTest {

    @Test
    public void testValidQuantity() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        int result = h.parseQuantity("25");
        System.out.println("testValidQuantity: Quantity parsed successfully -> " + result);
        assertEquals(25, result);
    }

    @Test
    public void testInvalidQuantityString() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        try {
            h.parseQuantity("abc");
            System.out.println("testInvalidQuantityString: FAIL (no exception thrown)");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("testInvalidQuantityString: PASS (caught " + e.getClass().getSimpleName() + ")");
        }
    }

    @Test
    public void testEmptyQuantityString() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        try {
            h.parseQuantity("   ");
            System.out.println("testEmptyQuantityString: FAIL (no exception thrown)");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("testEmptyQuantityString: PASS (caught " + e.getClass().getSimpleName() + ")");
        }
    }

    @Test
    public void testNullQuantityString() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        try {
            h.parseQuantity(null);
            System.out.println("testNullQuantityString: FAIL (no exception thrown)");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            System.out.println("testNullQuantityString: PASS (caught " + e.getClass().getSimpleName() + ")");
        }
    }

    @Test
    public void testFileNotFoundThrowsWarehouseException() {
        RethrowExceptionHandler h = new RethrowExceptionHandler();
        try {
            h.getFileSize("doesnotexist.txt");
            System.out.println("testFileNotFoundThrowsWarehouseException: FAIL (no exception thrown)");
            fail("Expected WarehouseException");
        } catch (RethrowExceptionHandler.WarehouseException e) {
            boolean valid = e.getMessage().contains("Error while reading file") && e.getCause() != null;
            System.out.println("testFileNotFoundThrowsWarehouseException: PASS (" + e.getMessage() + ")");
            assertTrue(valid);
        }
    }
}
