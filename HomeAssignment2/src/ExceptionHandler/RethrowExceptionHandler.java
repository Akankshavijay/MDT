package ExceptionHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class RethrowExceptionHandler {

    public int parseQuantity(String qtyText) {
        try {
            if (qtyText == null || qtyText.trim().isEmpty()) {
                throw new NumberFormatException("Empty or null quantity");
            }
            return Integer.parseInt(qtyText.trim());
        } catch (NumberFormatException e) {
            // Re-throw a more meaningful error
            throw new IllegalArgumentException("Invalid quantity format: " + qtyText, e);
        }
    }

    public long getFileSize(String path) throws WarehouseException {
        try {
            if (path == null || path.trim().isEmpty()) {
                throw new IOException("Invalid path");
            }

            File f = new File(path);
            if (!f.exists()) {
                throw new IOException("File not found: " + path);
            }

            try (FileInputStream fis = new FileInputStream(f)) {
                return fis.available();
            }
        } catch (IOException e) {
            throw new WarehouseException("Error while reading file: " + path, e);
        }
    }

    public static class WarehouseException extends Exception {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public WarehouseException(String message) {
            super(message);
        }

        public WarehouseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
