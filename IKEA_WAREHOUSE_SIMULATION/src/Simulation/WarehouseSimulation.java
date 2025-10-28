package Simulation;

import StorageManagement.*;
import RobotManager.*;
import warehouse_map.*;
import Logging.*;
import Exception_Handler.WarehouseException;

import java.io.*;
import java.util.*;

public class WarehouseSimulation {

    private static final String[] ITEM_TYPES = {
        "Chair", "Table", "Lamp", "Fan", "Sofa", "Desk", "Shelf", "Cabinet",
        "Bed", "Monitor", "Keyboard", "Box", "Router", "Carpet"
    };

    private static final int MAX_BINS = 100;
    private static final int SIMULATION_TIME_MIN = 5;
    private static final int NUM_LOADERS = 10;
    private static final int NUM_MOVERS = 15;
    private static final int NUM_UNLOADERS = 10;
    private static final Random rand = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("=== STREAM-BASED SMART WAREHOUSE SIMULATION STARTED ===");

        WarehouseException handler = new WarehouseException();
        LogManager logger = new LogManager();
        LogMetadataManager metaManager = new LogMetadataManager();
        WarehouseMap map = new WarehouseMap(5, 4, 6);

        StorageManager storage = new StorageManager("SmartStorageSystem", logger, handler, map, MAX_BINS);
        AGVManager agvManager = new AGVManager(logger, map, NUM_LOADERS, NUM_MOVERS, NUM_UNLOADERS);

        // ✅ Start the external Log Manager Console UI
        Thread logUI = new Thread(new LogManagerConsoleUI(metaManager));
        logUI.setDaemon(true);
        logUI.start();

        // Connect storage ↔ AGV streams
        PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        ObjectOutputStream oos = new ObjectOutputStream(pos);
        ObjectInputStream ois = new ObjectInputStream(pis);

        storage.connectStream(oos);
        agvManager.connectStream(ois);
        new Thread(agvManager).start();

        // Initialize bins
        for (int i = 1; i <= MAX_BINS; i++) {
            Bin bin = new Bin("B" + i, 10);
            storage.addBin(bin);
        }

        System.out.println("Warehouse initialized with " + MAX_BINS + " bins and " +
                (NUM_LOADERS + NUM_MOVERS + NUM_UNLOADERS) + " AGVs.\n");

        long simulationEnd = System.currentTimeMillis() + (SIMULATION_TIME_MIN * 60 * 1000);
        long lastSnapshot = System.currentTimeMillis();
        long start = System.currentTimeMillis();
        int eventCount = 0;

        // Simulation loop
        while (System.currentTimeMillis() < simulationEnd) {
            try {
                simulateRandomEvent(storage, logger);
                eventCount++;

                if (System.currentTimeMillis() - lastSnapshot >= 60000) {
                    logWarehouseSnapshot(map, logger, agvManager, eventCount);
                    lastSnapshot = System.currentTimeMillis();
                }

                if (eventCount % 20 == 0)
                    printLiveStatus(storage, agvManager, start);

                Thread.sleep(1000 + rand.nextInt(3000));
            } catch (Exception e) {
                handler.handleWarehouseOperation("WarehouseSimulation", () -> { throw new RuntimeException(e); });
            }
        }

        // Final logs
        logWarehouseSnapshot(map, logger, agvManager, eventCount);
        agvManager.stop();

        System.out.println("\n=== SIMULATION ENDED ===");
        System.out.println("Total events processed: " + eventCount);
        System.out.println("Subsystems logged: " + logger.listSubsystems());
        System.out.println("Logs available in /logs directory.");
    }

    // ------------------------------------------------------------
    private static void simulateRandomEvent(StorageManager storage, LogManager logger) {
        boolean storeOp = rand.nextBoolean();
        String binId = "B" + (1 + rand.nextInt(MAX_BINS));
        String itemType = ITEM_TYPES[rand.nextInt(ITEM_TYPES.length)];
        String itemId = itemType.substring(0, 2).toUpperCase() + "-" + rand.nextInt(10000);

        Item item = new Item(itemId, itemType);

        if (storeOp) {
            storage.requestStore(binId, item);
            logger.log("System", "STORE → " + itemType + " (" + itemId + ") → " + binId);
            System.out.println("STORE event: " + itemType + " → " + binId);
        } else {
            storage.requestRetrieve(binId, item);
            logger.log("System", "RETRIEVE → " + binId);
            System.out.println("RETRIEVE event triggered for " + binId);
        }
    }

    // ------------------------------------------------------------
    private static void logWarehouseSnapshot(WarehouseMap map, LogManager logger, AGVManager agvManager, int eventCount) {
        logger.log("WarehouseSnapshot", "=== Snapshot at Event #" + eventCount + " ===");
        logger.saveWarehouseSnapshot(map, "WarehouseSnapshot", agvManager.getRobots());
        logger.log("WarehouseSnapshot", "Snapshot saved successfully (" + eventCount + " events).");

        System.out.println("📦 Snapshot logged → " + eventCount + " events processed");
    }

    // ------------------------------------------------------------
    private static void printLiveStatus(StorageManager storage, AGVManager robotManager, long startTime) {
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        long mins = elapsed / 60, secs = elapsed % 60;

        System.out.println("\n--- LIVE STATUS [" + mins + "m " + secs + "s] ---");
        System.out.println("Active Robots: " + robotManager.getRobots().size());
        System.out.println("Discharge Queue: " + robotManager.getDischargeQueue().size());
        System.out.println("Charged Queue: " + robotManager.getChargedQueue().size());
        System.out.println("Low Battery Robots: " +
                robotManager.getRobots().stream().filter(AGV::isLowBattery).count());
        System.out.println("-----------------------------");
    }
}
