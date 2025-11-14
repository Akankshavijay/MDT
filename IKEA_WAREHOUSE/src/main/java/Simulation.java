package main.java;

import main.java.chargingManagement.ChargingManager;
import main.java.chargingManagement.ChargingStation;
import main.java.dashboard.*;
import main.java.logging.LogManager;
import main.java.robotManagement.Robot;
import main.java.robotManagement.RobotManager;
import main.java.storageManagement.Bin;
import main.java.storageManagement.StorageManager;
import main.java.taskManager.TaskManager;
import main.java.taskManager.TaskType;
import main.java.taskManager.WarehouseTask;

import java.io.File;
import java.util.Arrays;

public class Simulation {
    public static final int SPEED_1X = 1;
    public static final int SPEED_2X = 2;
    public static final int SPEED_4X = 4;
    public static final int SPEED_8X = 8;
    
    private static volatile int simulationSpeed = SPEED_1X;
    
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

		Bin b1 = new Bin("B1", 1, 1);
		Bin b2 = new Bin("B2", 2, 1);

		StorageManager storageManager = new StorageManager("StorageManager", logger, Arrays.asList(b1, b2));

		ChargingManager chargingManager = new ChargingManager("ChargingManager", logger);

		ChargingStation station1 = new ChargingStation("CS1", 0, 0, "ChargingManager", logger);
		chargingManager.addStation(station1);

		RobotManager robotManager = new RobotManager("RobotManager", logger);
		robotManager.setChargingManager(chargingManager);

		Robot r1 = new Robot("R1", 0, 0, 100, "RobotManager", logger);
		Robot r2 = new Robot("R2", 0, 0, 30, "RobotManager", logger);

		robotManager.addRobot(r1);
		robotManager.addRobot(r2);

		File snapshotDir = new File("target/tasksnapshots");
		if (!snapshotDir.exists()) {
			snapshotDir.mkdirs();
		}

		TaskManager taskManager = new TaskManager("TaskManager", logger, storageManager, robotManager, snapshotDir);

		WarehouseTask tStore1 = new WarehouseTask("TS1", TaskType.STORE, "B1", "I1", "BOX");

		WarehouseTask tStore2 = new WarehouseTask("TS2", TaskType.STORE, null, "I2", "BOX");

		try {
			taskManager.submit(tStore1);
			taskManager.submit(tStore2);
		} catch (Exception e) {
			logger.log("Simulation", "Failed to submit initial tasks: " + e.getMessage());
		}

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
				station1.stop();
				r1.stop();
				r2.stop();
			} catch (Throwable t) {
				logger.log("Simulation", "Error during worker shutdown: " + t.getMessage());
			}

			logger.log("Simulation", "Shutdown sequence finished.");
		}, "Simulation-ShutdownHook"));

		Thread dashboardThread = new Thread(() -> {
			Dashboard.launchDashboard(robotManager, storageManager, taskManager, logger);
		}, "Dashboard-Thread");

		dashboardThread.start();

		logger.log("Simulation", "Starting managers...");
		storageManager.start();
		chargingManager.start();
		robotManager.start();
		taskManager.start();
		logger.log("Simulation", "Simulation started. Press Ctrl+C to exit.");

		try {
			while (true) {
				Thread.sleep(1000L);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		logger.log("Simulation", "Main thread exiting.");
	}
}
