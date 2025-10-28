package RobotManager;

import Communication.WarehouseMessage;
import Logging.LogManager;
import warehouse_map.WarehouseMap;

import java.io.*;
import java.util.*;

public class AGVManager implements Runnable {
    private final LogManager logger;
    private final List<AGV> robots = new ArrayList<>();
    private final Queue<AGV> dischargeQueue = new LinkedList<>();
    private final Queue<AGV> chargedQueue = new LinkedList<>();
    private ObjectInputStream inStream;
    private volatile boolean running = true;

    public AGVManager(LogManager logger, WarehouseMap map, int loaders, int movers, int unloaders) {
        this.logger = logger;
        initializeRobots(loaders, movers, unloaders);
    }

    private void initializeRobots(int loaders, int movers, int unloaders) {
        for (int i = 1; i <= loaders; i++) robots.add(new AGV("L" + i, "loader", logger));
        for (int i = 1; i <= movers; i++) robots.add(new AGV("M" + i, "mover", logger));
        for (int i = 1; i <= unloaders; i++) robots.add(new AGV("U" + i, "unloader", logger));
        logger.log("RobotSystem", "Initialized " + robots.size() + " AGVs (L=" + loaders + ", M=" + movers + ", U=" + unloaders + ")");
    }

    public void connectStream(ObjectInputStream stream) {
        this.inStream = stream;
    }

    @Override
    public void run() {
        logger.log("RobotSystem", "AGVManager stream listener started.");
        while (running) {
            try {
                WarehouseMessage msg = (WarehouseMessage) inStream.readObject();
                if (msg != null) handleMessage(msg);
            } catch (EOFException e) {
                // Graceful end of stream
            } catch (Exception e) {
                logger.log("RobotSystem", "Stream read error: " + e.getMessage());
            }
        }
        logger.log("RobotSystem", "AGVManager listener stopped.");
    }

    private void handleMessage(WarehouseMessage msg) {
        logger.log("RobotSystem", "Stream ← Received: " + msg.toString());
        executeTask(msg);
    }

    private void executeTask(WarehouseMessage msg) {
        AGV loader = pickRobot("loader");
        AGV mover = pickRobot("mover");
        AGV unloader = pickRobot("unloader");

        if (loader == null || mover == null || unloader == null) {
            logger.log("RobotSystem", "⚠️ No available robots for task: " + msg);
            return;
        }

        // Start the three AGVs on separate threads
        new Thread(() -> {
            loader.execute(msg.toString(), 0, 0, 5);
            handleBatteryAfterTask(loader);
        }).start();

        new Thread(() -> {
            mover.execute(msg.toString(), 10, 10, 2);
            handleBatteryAfterTask(mover);
        }).start();

        new Thread(() -> {
            unloader.execute(msg.toString(), 0, 0, 7);
            handleBatteryAfterTask(unloader);
        }).start();
    }

    // ✅ Automatically move AGVs with low battery to discharge queue
    private void handleBatteryAfterTask(AGV agv) {
        if (agv.isLowBattery()) {
            synchronized (dischargeQueue) {
                if (!dischargeQueue.contains(agv)) {
                    dischargeQueue.add(agv);
                    logger.log("RobotSystem",
                            "⚠️ AGV " + agv.getId() + " (" + agv.getType() + 
                            ") low battery: " + agv.getBatteryLevel() + "%. → Sent to discharge queue.");
                }
            }
        }
    }

    private AGV pickRobot(String type) {
        return robots.stream()
                .filter(r -> r.getType().equals(type) && r.isFree())
                .findAny()
                .orElse(null);
    }

    // Public getters for monitoring
    public List<AGV> getRobots() { return robots; }
    public Queue<AGV> getDischargeQueue() { return dischargeQueue; }
    public Queue<AGV> getChargedQueue() { return chargedQueue; }

    public void addChargedRobot(AGV agv) {
        synchronized (chargedQueue) {
            if (!chargedQueue.contains(agv)) {
                chargedQueue.add(agv);
                logger.log("RobotSystem", "🔋 AGV " + agv.getId() + " fully charged and added back to charged queue.");
            }
        }
    }

    public void stop() {
        running = false;
    }
}
