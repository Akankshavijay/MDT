package Exception_Handler.tests;

import Exception_Handler.MultipleExceptionsHandler;
import org.junit.Test;
import static org.junit.Assert.*;

public class MultipleExceptionsHandlerTest {

    @Test
    public void testValidOrder() {
        MultipleExceptionsHandler h = new MultipleExceptionsHandler();
        int[] inv = {10, 5};
        String result = h.processOrder(inv, 1, 3);
        assertEquals("OK", result);
        assertEquals(2, inv[1]);
    }

    @Test
    public void testNullInventory() {
        MultipleExceptionsHandler h = new MultipleExceptionsHandler();
        String result = h.processOrder(null, 0, 1);
        assertEquals("ERR_NULL", result);
    }

    @Test
    public void testInvalidIndex() {
        MultipleExceptionsHandler h = new MultipleExceptionsHandler();
        int[] inv = {5};
        String result = h.processOrder(inv, 3, 1);
        assertEquals("ERR_INDEX", result);
    }

    @Test
    public void testInvalidQuantity() {
        MultipleExceptionsHandler h = new MultipleExceptionsHandler();
        int[] inv = {5};
        String result = h.processOrder(inv, 0, 0);
        assertEquals("ERR_QTY", result);
    }

    @Test
    public void testInsufficientStock() {
        MultipleExceptionsHandler h = new MultipleExceptionsHandler();
        int[] inv = {2};
        String result = h.processOrder(inv, 0, 5);
        assertEquals("ERR_STOCK", result);
    }
}
