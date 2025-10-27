package StorageManagement;

import Exception_Handler.WarehouseException;
import Logging.*;
import java.io.*;
import java.time.*;
import java.util.*;
import warehouse_map.*;

public class StorageSystemTest {

    public static void main(String[] args) {
        WarehouseException handler = new WarehouseException();
        LogManager logger = new LogManager();
        WarehouseMap map = new WarehouseMap(2, 2, 3);
        
        StorageManager storage = new StorageManager("SmartStorageSystem", logger, handler, map, 5);

        System.out.println("STORAGE MANAGEMENT TEST");

        try {
            testAddBins(storage);
            testStoreItems(storage);
            testBinStatus(storage);
            testRemoveItems(storage);
            testInvalidBin(storage, handler);
            testStreamSimulation(storage, logger, handler);
            testMaxBinLimit(logger, handler);
            testLoggerVerification(logger);

        } catch (Exception e) {
            handler.handleWarehouseOperation("MainTestSuite", () -> { throw new RuntimeException(e); });
        }
    }

    private static void testAddBins(StorageManager storage) {
        System.out.println("\n[Test1] Adding bins");
        storage.addBin(new Bin("B1", 10));
        storage.addBin(new Bin("B2", 5));
        storage.addBin(new Bin("B3", 8));
        System.out.println("Bins added successfully. Total: " + storage.getTotalBins());
        

        storage.getWarehouseMap().printMap();
    }

    private static void testStoreItems(StorageManager storage) {
        System.out.println("\n[Test2] Storing items");
        storage.storeItem("B1", new Item("I001", "Chair"));
        storage.storeItem("B2", new Item("I002", "Table"));
        storage.storeItem("B3", new Item("I003", "Sofa"));
        System.out.println("Items stored successfully.");
    }

    private static void testBinStatus(StorageManager storage) {
        System.out.println("\n[Test3] Checking bin occupancy");
        System.out.println("B1 occupied? " + storage.isBinOccupied("B1"));
        System.out.println("B2 occupied? " + storage.isBinOccupied("B2"));
        System.out.println("B3 occupied? " + storage.isBinOccupied("B3"));
        System.out.println("Bin status check successful.");
    }

    private static void testRemoveItems(StorageManager storage) {
        System.out.println("\n[Test4] Removing items");
        storage.removeItem("B1");
        storage.removeItem("B2");
        storage.removeItem("B3");
        System.out.println("Items removed successfully.");
    }

    private static void testInvalidBin(StorageManager storage, WarehouseException handler) {
        System.out.println("\n[Test5] Testing invalid bin operation");
        handler.handleWarehouseOperation("InvalidBinTest", () -> {
            storage.storeItem("INVALID", new Item("I004", "Lamp"));
        });
        System.out.println("Invalid bin handled gracefully.");
    }

    private static void testStreamSimulation(StorageManager storage, LogManager logger, WarehouseException handler) {
        System.out.println("\n[Test6] Simulating data exchange (Byte Streams)");
        handler.handleWarehouseOperation("StreamSimulation", () -> {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                 ObjectOutputStream oos = new ObjectOutputStream(bos)) {

                String[] mockData = {"Task1", "Task2", "Task3"};
                oos.writeObject(mockData);
                oos.flush();

                ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                try (ObjectInputStream ois = new ObjectInputStream(bis)) {
                    Object data = ois.readObject();
                    logger.log("SmartStorageSystem", "stream data exchange: " +
                            Arrays.toString((Object[]) data));
                    System.out.println("Data stream simulation successful.");
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException("Stream simulation failed: " + e.getMessage());
            }
        });
    }

    private static void testMaxBinLimit(LogManager logger, WarehouseException handler) {
        System.out.println("\n[Test7] warehouse max bin capacity");
        WarehouseMap smallMap = new WarehouseMap(1, 1, 3);
        StorageManager limitedStorage = new StorageManager("Warehouse", logger, handler, smallMap, 3);

        limitedStorage.addBin(new Bin("X1", 10));
        limitedStorage.addBin(new Bin("X2", 8));
        limitedStorage.addBin(new Bin("X3", 12));

        handler.handleWarehouseOperation("MaxLimitTest", () -> {
            limitedStorage.addBin(new Bin("X4", 5));
        });

        System.out.println("Max bin limit enforced correctly (capacity: " + limitedStorage.getMaxBins() + ")");
    }

    private static void testLoggerVerification(LogManager logger) {
        System.out.println("\n[Test8] Verifying logging functionality...");

        String subsystem = "SmartStorageSystem";
        String today = LocalDate.now().toString();

        System.out.println("Available subsystems: " + logger.listSubsystems());
        System.out.println("Log files for subsystem '" + subsystem + "': " + logger.listLogs(subsystem));

        String logContent = logger.openLog(subsystem, today);

        if (logContent.contains("Added bin") || logContent.contains("Stored") || logContent.contains("Removed")) {
            System.out.println("Log verification successful.");
        } else {
            System.out.println("Log content verification failed");
        }

        System.out.println("\n Log Preview (" + subsystem + "/" + today + ")");
        System.out.println(logContent.lines().limit(10).reduce("", (a, b) -> a + "\n" + b));
    }



}
