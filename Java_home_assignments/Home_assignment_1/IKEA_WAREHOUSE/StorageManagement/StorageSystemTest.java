package StorageManagement;

import Exception_Handler.WarehouseException;
import Logging.*;
import Communication.WarehouseMessage;
import warehouse_map.*;

import java.io.*;
import java.util.*;

public class StorageSystemTest {

    public static void main(String[] args) {
        WarehouseException handler = new WarehouseException();
        LogManager logger = new LogManager();
        WarehouseMap map = new WarehouseMap(2, 2, 3);

        StorageManager storage = new StorageManager("SmartStorageSystem", logger, handler, map, 5);

        System.out.println("=== STORAGE MANAGEMENT UNIT TEST SUITE (STREAM-BASED) ===");

        try {
            testAddBins(storage);
            testStreamConnectionAndRequests(storage, logger, handler);
            testIsBinOccupied(storage);
            testMaxBinLimit(logger, handler);
            System.out.println("\n✅ All storage manager tests executed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------
    private static void testAddBins(StorageManager storage) {
        System.out.println("\n[Test1] Adding bins to warehouse");
        storage.addBin(new Bin("B1", 10));
        storage.addBin(new Bin("B2", 10));
        storage.addBin(new Bin("B3", 10));
        storage.addBin(new Bin("B4", 10));
        System.out.println("Bins added successfully. Total bins: 4");

    }

    // ----------------------------------------------------------------
    @SuppressWarnings("resource")
	private static void testStreamConnectionAndRequests(StorageManager storage, LogManager logger, WarehouseException handler) {
        System.out.println("\n[Test2] Testing stream-based communication");

        try {
            // Connect Object streams (simulate AGV/Robot system receiver)
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            ObjectOutputStream oos = new ObjectOutputStream(pos);
            ObjectInputStream ois = new ObjectInputStream(pis);

            // Attach to storage manager
            storage.connectStream(oos);

            // Spawn a thread to simulate receiver (AGV system)
            Thread receiver = new Thread(() -> {
                try {
                    Object obj;
                    while ((obj = ois.readObject()) != null) {
                        if (obj instanceof WarehouseMessage msg) {
                            System.out.println("[Receiver] Received message → " + msg);
                        }
                    }
                } catch (EOFException eof) {
                    System.out.println("[Receiver] Stream closed.");
                } catch (Exception e) {
                    System.out.println("[Receiver] Error reading stream: " + e.getMessage());
                }
            });
            receiver.start();

            // Send test store/retrieve requests
            storage.requestStore("B1", new Item("I001", "Chair"));
            Thread.sleep(1000);
            storage.requestStore("B2", new Item("I002", "Table"));
            Thread.sleep(1000);
            storage.requestRetrieve("B1", new Item("I001", "Chair"));
            Thread.sleep(1000);

            // Cleanup
            oos.close();
            receiver.join();
            System.out.println("Stream communication test completed successfully.");

        } catch (Exception e) {
            handler.handleWarehouseOperation("StreamConnectionTest", () -> { throw new RuntimeException(e); });
        }
    }

    // ----------------------------------------------------------------
    private static void testIsBinOccupied(StorageManager storage) {
        System.out.println("\n[Test3] Checking bin occupancy");
        System.out.println("B1 occupied? " + storage.isBinOccupied("B1"));
        System.out.println("B2 occupied? " + storage.isBinOccupied("B2"));
        System.out.println("B3 occupied? " + storage.isBinOccupied("B3"));
        System.out.println("Bin occupancy check successful.");
    }

    // ----------------------------------------------------------------
    private static void testMaxBinLimit(LogManager logger, WarehouseException handler) {
        System.out.println("\n[Test4] Warehouse max bin capacity limit");

        WarehouseMap smallMap = new WarehouseMap(1, 1, 2);
        StorageManager limitedStorage = new StorageManager("LimitedWarehouse", logger, handler, smallMap, 3);

        limitedStorage.addBin(new Bin("X1", 10));
        limitedStorage.addBin(new Bin("X2", 8));
        limitedStorage.addBin(new Bin("X3", 12));

        handler.handleWarehouseOperation("MaxLimitTest", () -> {
            limitedStorage.addBin(new Bin("X4", 5));
        });

        logger.log("System", "Warehouse snapshot saved.");
    }
}
