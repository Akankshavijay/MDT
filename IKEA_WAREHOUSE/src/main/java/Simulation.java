package main.java;

import main.java.chargingManagement.ChargingManager;
import main.java.chargingManagement.ChargingStation;
import main.java.dashboard.Dashboard;
import main.java.logging.LogManager;
import main.java.robotManagement.Robot;
import main.java.robotManagement.RobotManager;
import main.java.storageManagement.Bin;
import main.java.storageManagement.StorageManager;
import main.java.taskManager.TaskManager;
import main.java.taskManager.TaskType;
import main.java.taskManager.WarehouseTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Simulation {

    public static final int SPEED_1X = 1;
    public static final int SPEED_2X = 2;
    public static final int SPEED_4X = 4;
    public static final int SPEED_8X = 8;

    private static volatile int simulationSpeed = SPEED_1X;

    private static final int BIN_ROWS = 4;
    private static final int BIN_COLS = 4;
    private static final int ROBOT_COUNT = 10;
    private static final int CHARGING_STATION_COUNT = 3;
    private static final int INITIAL_TASK_COUNT = 10;

    public static int getSimulationSpeed() {
        int s = simulationSpeed;
        return (s == SPEED_1X || s == SPEED_2X || s == SPEED_4X || s == SPEED_8X)
                ? s
                : SPEED_1X;
    }

    public static void setSimulationSpeed(int speed) {
        if (speed != SPEED_1X && speed != SPEED_2X && speed != SPEED_4X && speed != SPEED_8X) {
            speed = SPEED_1X;
        }
        simulationSpeed = speed;
    }

    public static void main(String[] args) throws Exception {
        LogManager logger = new LogManager();
        logger.log("Simulation", "Bootstrapping simulation...");

        // --------------------------------------------------------------------
        // 1. Create Bins
        // --------------------------------------------------------------------
        List<Bin> bins = createBinsGrid(BIN_ROWS, BIN_COLS);
        logger.log("Simulation", "Created " + bins.size() + " bins.");

        StorageManager storageManager = new StorageManager("StorageManager", logger, bins);

        // --------------------------------------------------------------------
        // 2. Create Charging Manager & Stations
        // --------------------------------------------------------------------
        ChargingManager chargingManager = new ChargingManager("ChargingManager", logger);
        List<ChargingStation> stations = createChargingStations(
                CHARGING_STATION_COUNT,
                chargingManager,
                logger
        );
        logger.log("Simulation", "Created " + stations.size() + " charging stations.");

        // --------------------------------------------------------------------
        // 3. Create Robot Manager & Robots
        // --------------------------------------------------------------------
        RobotManager robotManager = new RobotManager("RobotManager", logger);
        robotManager.setChargingManager(chargingManager);

        List<Robot> robots = createRobots(
                ROBOT_COUNT,
                robotManager,
                logger
        );
        logger.log("Simulation", "Created " + robots.size() + " robots.");

        // --------------------------------------------------------------------
        // 4. Task Manager & Snapshots
        // --------------------------------------------------------------------
        File snapshotDir = new File("target/tasksnapshots");
        if (!snapshotDir.exists()) {
            snapshotDir.mkdirs();
        }

        TaskManager taskManager = new TaskManager(
                "TaskManager",
                logger,
                storageManager,
                robotManager,
                snapshotDir
        );

        // --------------------------------------------------------------------
        // 5. Create & Submit Initial STORE + RETRIEVE Tasks
        // --------------------------------------------------------------------
        submitInitialTasks(taskManager, bins, logger, INITIAL_TASK_COUNT);

        // --------------------------------------------------------------------
        // 6. Shutdown Hook
        // --------------------------------------------------------------------
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.log("Simulation", "Shutdown requested, stopping managers and workers...");
            try {
                taskManager.stop();
                robotManager.stop();
                chargingManager.stop();
                storageManager.stop();
            } catch (Throwable t) {
                logger.log("Simulation", "Error during manager shutdown: " + t.getMessage());
            }

            try {
                for (ChargingStation station : stations) {
                    try {
                        station.stop();
                    } catch (Throwable t) {
                        logger.log("Simulation", "Error stopping station " + station.getId() + ": " + t.getMessage());
                    }
                }

                for (Robot robot : robots) {
                    try {
                        robot.stop();
                    } catch (Throwable t) {
                        logger.log("Simulation", "Error stopping robot " + robot.getId() + ": " + t.getMessage());
                    }
                }
            } catch (Throwable t) {
                logger.log("Simulation", "Error during worker shutdown: " + t.getMessage());
            }

            logger.log("Simulation", "Shutdown sequence finished.");
        }, "Simulation-ShutdownHook"));

        // --------------------------------------------------------------------
        // 7. Start Dashboard
        // --------------------------------------------------------------------
        Thread dashboardThread = new Thread(() -> {
            Dashboard.launchDashboard(robotManager, storageManager, taskManager, logger);
        }, "Dashboard-Thread");
        dashboardThread.start();

        // --------------------------------------------------------------------
        // 8. Start Managers
        // --------------------------------------------------------------------
        logger.log("Simulation", "Starting managers...");
        storageManager.start();
        chargingManager.start();
        robotManager.start();
        taskManager.start();
        logger.log("Simulation", "Simulation started. Press Ctrl+C to exit.");

        // --------------------------------------------------------------------
        // 9. Main Loop (speed aware)
        // --------------------------------------------------------------------
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long baseSleepMillis = 1000L;
                int speed = getSimulationSpeed();
                long sleepTime = baseSleepMillis / speed;
                if (sleepTime <= 0) sleepTime = 1;
                Thread.sleep(sleepTime);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        logger.log("Simulation", "Main thread exiting.");
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    private static List<Bin> createBinsGrid(int rows, int cols) {
    	int offsetX = 10;
    	int offsetY = 0;
    	
        List<Bin> bins = new ArrayList<>(rows * cols);
        for (int y = 0 + offsetY; y < rows + offsetY; y++) {
            for (int x = 0 + offsetX; x < cols + offsetX; x++) {
                String id = "B" + y + "_" + x;
                bins.add(new Bin(id, x, y));
            }
        }
        return bins;
    }

    private static List<ChargingStation> createChargingStations(
            int count,
            ChargingManager chargingManager,
            LogManager logger
    ) {
        List<ChargingStation> stations = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = "CS" + (i + 1);
            int x = i * 2;
            int y = 0;
            ChargingStation station = new ChargingStation(id, x, y, "ChargingManager", logger);
            chargingManager.addStation(station);
            stations.add(station);
        }
        return stations;
    }

    private static List<Robot> createRobots(
            int count,
            RobotManager robotManager,
            LogManager logger
    ) {
        List<Robot> robots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String id = "R" + (i + 1);
            int x = i % 5;
            int y = i / 5;
            int battery = 30 + (i * 7) % 70;

            Robot robot = new Robot(id, x, y, battery, "RobotManager", logger);
            robotManager.addRobot(robot);
            robots.add(robot);
        }
        return robots;
    }

    /**
     * Submit STORE tasks followed by RETRIEVE tasks for the same items.
     */
    private static void submitInitialTasks(
            TaskManager taskManager,
            List<Bin> bins,
            LogManager logger,
            int taskCount
    ) {
        int binCount = bins.size();
        if (binCount == 0) {
            logger.log("Simulation", "No bins available, cannot submit initial tasks.");
            return;
        }

        // -------------------------
        // 1. STORE TASKS
        // -------------------------
        for (int i = 0; i < taskCount; i++) {
            Bin targetBin = bins.get(i % binCount);
            String taskId = "TS" + (i + 1);
            String binId = targetBin.getId();
            String itemId = "I" + (i + 1);
            String itemType = "BOX";

            WarehouseTask task = new WarehouseTask(
                    taskId,
                    TaskType.STORE,
                    binId,
                    itemId,
                    itemType
            );

            try {
                taskManager.submit(task);
            } catch (Exception e) {
                logger.log("Simulation",
                        "Failed to submit STORE task " + taskId + ": " + e.getMessage());
            }
        }

        logger.log("Simulation", "Submitted " + taskCount + " STORE tasks.");

        // -------------------------
        // 2. RETRIEVE TASKS
        // -------------------------
        for (int i = 0; i < taskCount; i++) {
            Bin targetBin = bins.get(i % binCount);
            String binId = targetBin.getId();
            String itemId = "I" + (i + 1);
            String taskId = "TR" + (i + 1);

            WarehouseTask retrieveTask = new WarehouseTask(
                    taskId,
                    TaskType.RETRIEVE,
                    binId,
                    itemId,
                    null
            );

            try {
                taskManager.submit(retrieveTask);
            } catch (Exception e) {
                logger.log("Simulation",
                        "Failed to submit RETRIEVE task " + taskId + ": " + e.getMessage());
            }
        }

        logger.log("Simulation", "Submitted " + taskCount + " RETRIEVE tasks.");
    }
}
