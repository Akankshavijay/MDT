package RobotManager;

import Logging.*;
import warehouse_map.*;
import Communication.WarehouseMessage;

import java.io.*;
import java.util.*;

public class AgvTest {

    public static void main(String[] args) {
        LogManager logger = new LogManager();
        WarehouseMap map = new WarehouseMap(2, 2, 3);
        AGVManager manager = new AGVManager(logger, map, 2, 3, 2);

        System.out.println("=== ROBOT MANAGER UNIT TEST SUITE (STREAM-BASED) ===");

        try {
            testInitialization(manager);
            testStreamTaskFlow(manager, logger, map);
            testBatteryDrain(manager);
            testChargingQueue(manager);
            testConcurrentStreamTasks(manager, logger, map);

            System.out.println("\n✅ All robot manager stream-based tests passed successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------
    private static void testInitialization(AGVManager manager) {
        System.out.println("\n[Test1] Initialization and robot count");
        List<AGV> robots = manager.getRobots();
        System.out.println("Total robots initialized: " + robots.size());
        assert !robots.isEmpty() : "Robot list is empty!";
        robots.forEach(r -> System.out.println(" → " + r.getId() + " (" + r.getType() + ") Battery=" + r.getBatteryLevel() + "%"));
    }

    // ----------------------------------------------------
    private static void testStreamTaskFlow(AGVManager manager, LogManager logger, WarehouseMap map) {
        System.out.println("\n[Test2] Testing AGV stream-based task flow");

        try {
            // Connect Piped Streams
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            ObjectOutputStream oos = new ObjectOutputStream(pos);
            ObjectInputStream ois = new ObjectInputStream(pis);

            manager.connectStream(ois);
            new Thread(manager).start();

            // Simulate sending warehouse messages (store + retrieve)
            oos.writeObject(new WarehouseMessage("store", "B1", "I001", "Chair"));
            Thread.sleep(3000);
            oos.writeObject(new WarehouseMessage("retrieve", "B2", "I002", "Table"));
            Thread.sleep(4000);

            oos.close();
            manager.stop();
            System.out.println("Stream-based AGV task flow executed successfully.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------
    private static void testBatteryDrain(AGVManager manager) {
        System.out.println("\n[Test3] Battery drain simulation");

        AGV testAGV = manager.getRobots().get(0);
        System.out.println("Initial battery: " + testAGV.getBatteryLevel() + "%");
        testAGV.execute("Simulated Heavy Task", 50, 50, 8);
        System.out.println("After heavy work: " + testAGV.getBatteryLevel() + "%");
        assert testAGV.getBatteryLevel() < 100 : "Battery did not drain!";
    }

    // ----------------------------------------------------
    private static void testChargingQueue(AGVManager manager) {
        System.out.println("\n[Test4] Discharge and recharge queue flow");

        AGV lowRobot = manager.getRobots().get(0);
        if (!lowRobot.isLowBattery()) {
            lowRobot.execute("DrainBattery", 100, 100, 10);
        }

        if (lowRobot.isLowBattery()) {
            manager.getDischargeQueue().add(lowRobot);
            System.out.println("Robot " + lowRobot.getId() + " added to discharge queue.");
        }

        System.out.println("Discharge queue size: " + manager.getDischargeQueue().size());
        assert manager.getDischargeQueue().size() > 0 : "Discharge queue empty!";

        // Simulate charging
        manager.getDischargeQueue().poll();
        manager.getChargedQueue().add(lowRobot);

        System.out.println("Robot " + lowRobot.getId() + " charged, new battery: " + lowRobot.getBatteryLevel() + "%");
        assert manager.getChargedQueue().size() > 0 : "Charged queue empty!";
    }

    // ----------------------------------------------------
    private static void testConcurrentStreamTasks(AGVManager manager, LogManager logger, WarehouseMap map) {
        System.out.println("\n[Test5] Concurrent stream task execution");

        try {
            PipedOutputStream pos = new PipedOutputStream();
            PipedInputStream pis = new PipedInputStream(pos);
            ObjectOutputStream oos = new ObjectOutputStream(pos);
            ObjectInputStream ois = new ObjectInputStream(pis);

            manager.connectStream(ois);
            new Thread(manager).start();

            for (int i = 0; i < 5; i++) {
                String action = (i % 2 == 0) ? "store" : "retrieve";
                String bin = "B" + (i + 1);
                oos.writeObject(new WarehouseMessage(action, bin, "I00" + i, "Box"));
                Thread.sleep(1000);
            }

            oos.close();
            Thread.sleep(7000);
            manager.stop();
            System.out.println("Concurrent stream task execution successful.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
