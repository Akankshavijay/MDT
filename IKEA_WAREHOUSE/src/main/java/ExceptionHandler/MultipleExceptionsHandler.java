package main.java.ExceptionHandler;

import java.util.Objects;

public class MultipleExceptionsHandler {

    public String processOrder(int[] inventory, int index, int quantity) {
        try {
            Objects.requireNonNull(inventory, "Inventory is null");

            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive");
            }

            // may throw ArrayIndexOutOfBoundsException
            int current = inventory[index];

            if (current < quantity) {
                throw new IllegalArgumentException("Not enough stock");
            }

            inventory[index] = current - quantity;
            System.out.println("Order processed successfully for index " + index +
                    " (deducted " + quantity + " items)");
            return "OK";

        } catch (NullPointerException e) {
            System.out.println("Null input error: " + e.getMessage());
            return "ERR_NULL";

        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Invalid index error: " + e.getMessage());
            return "ERR_INDEX";

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("quantity")) {
                System.out.println("Bad quantity error: " + e.getMessage());
                return "ERR_QTY";
            } else {
                System.out.println("Stock problem: " + e.getMessage());
                return "ERR_STOCK";
            }

        } catch (Exception e) {
            System.out.println("Unknown exception: " + e.getMessage());
            return "ERR_UNKNOWN";
        }
    }
}
