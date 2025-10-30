package ExceptionHandler;


public class MultipleExceptionsHandler {

    public String processOrder(int[] inventory, int index, int quantity) {
        try {
            // Check if inventory is null
            if (inventory == null) {
                throw new NullPointerException("Inventory is null");
            }

            // Check for invalid quantity
            if (quantity <= 0) {
                throw new IllegalArgumentException("QuanTity must be positive");
            }

            // throw ArrayIndexOutOfBoundsException
            int current = inventory[index];

            // Check if stock is available
            if (current < quantity) {
                throw new IllegalArgumentException("Not enough stock");
            }

            // Reduce stock and return success
            inventory[index] = current - quantity;
            return "OK";

        } catch (NullPointerException e) {
            System.out.println("Null input: " + e.getMessage());
            return "ERR_NULL";

        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Invalid index: " + e.getMessage());
            return "ERR_INDEX";

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage().toLowerCase();
            if (msg.contains("quantity")) {
                System.out.println("Bad quantity: " + e.getMessage());
                return "ERR_QTY";
            } else {
                System.out.println("Stock problem: " + e.getMessage());
                return "ERR_STOCK";
            }
        }
    }
}
