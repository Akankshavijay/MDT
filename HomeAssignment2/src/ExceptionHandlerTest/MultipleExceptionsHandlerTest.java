package ExceptionHandlerTest;

import ExceptionHandler.MultipleExceptionsHandler;

public class MultipleExceptionsHandlerTest {

    public static void main(String[] args) {
        MultipleExceptionsHandler h = new MultipleExceptionsHandler();

        // Test 1: Valid order
        int[] inv1 = {10, 5};
        String result1 = h.processOrder(inv1, 1, 3);
        System.out.println("Test 1: " + ("OK".equals(result1) && inv1[1] == 2));

        // Test 2: Null inventory
        String result2 = h.processOrder(null, 0, 1);
        System.out.println("Test 2: " + "ERR_NULL".equals(result2));

        // Test 3: Invalid index
        int[] inv3 = {5};
        String result3 = h.processOrder(inv3, 3, 1);
        System.out.println("Test 3: " + "ERR_INDEX".equals(result3));

        // Test 4: Invalid quantity
        int[] inv4 = {5};
        String result4 = h.processOrder(inv4, 0, 0);
        System.out.println("Test 4: " + "ERR_QTY".equals(result4));

        // Test 5: Insufficient stock
        int[] inv5 = {2};
        String result5 = h.processOrder(inv5, 0, 5);
        System.out.println("Test 5: " + "ERR_STOCK".equals(result5));
    }
}
