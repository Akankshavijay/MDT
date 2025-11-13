package test.java.integrationTests;

import main.java.logging.LogManager;
import main.java.chargingManagement.*;
import main.java.robotManagement.*;
import main.java.robotManagement.Robot.Status;
import main.java.storageManagement.*;
import main.java.taskManager.*;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Supplier;

public class IntegrationTests {

    private void waitUntil(Supplier<Boolean> condition,
                           long timeoutMillis,
                           String failMessage) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return;
            }
            Thread.sleep(50L);
        }
        fail(failMessage);
    }

    @Test
    public void chargingMngEndToEndTest() throws Exception {
        LogManager logger = new LogManager();

        ChargingManager chargingManager = new ChargingManager("ChargingManager", logger);
        ChargingStation station = new ChargingStation("S1", 0, 0, "ChargingStation", logger);
        chargingManager.addStation(station);

        Robot robot = new Robot("R1", "Robot", logger);
        robot.setBattery(10);

        chargingManager.addRobotToQueue(robot);

        Thread robotThread = new Thread(robot, "Robot-R1");
        robotThread.start();
        chargingManager.start();

        waitUntil(
                () -> robot.getBattery() > 10
                        && station.getCurrentRobot() == null
                        && station.getStatus() == ChargingStation.Status.READY,
                15_000L,
                "Robot should be charged and station should be READY again"
        );

        chargingManager.stop();
        robot.stop();
        robotThread.join(2000L);

        assertTrue(robot.getBattery() > 10, "Battery should have increased after charging");
        assertEquals(Robot.Status.READY, robot.getStatus());
        assertEquals(ChargingStation.Status.READY, station.getStatus());
        assertNull(station.getCurrentRobot());
    }
    
    @Test
    public void robotMngAndChargingMngEndToEndTest() throws Exception {
        LogManager logger = new LogManager();

        ChargingManager chargingManager = new ChargingManager("ChargingManager", logger);
        ChargingStation station = new ChargingStation("S1", 0, 0, "ChargingStation", logger);
        chargingManager.addStation(station);

        RobotManager robotManager = new RobotManager("RobotManager", logger);
        robotManager.setChargingManager(chargingManager);

        Robot robot = new Robot("R1", "Robot", logger);
        robot.setBattery(10);
        robotManager.addRobot(robot);

        WarehouseTask dummyTask = new WarehouseTask("D1", TaskType.STORE, "B1", "IDUMMY", "BOX");

        chargingManager.start();
        robotManager.start();

        robotManager.enqueueRobotTask(
                RobotTask.moveTo(5, 5),
                dummyTask
        );

        waitUntil(
                () -> robot.getBattery() > 10
                        && robot.getStatus() == Robot.Status.READY
                        && station.getCurrentRobot() == null
                        && station.getStatus() == ChargingStation.Status.READY,
                20_000L,
                "Robot should have been charged via RobotManager + ChargingManager"
        );

        chargingManager.stop();
        robotManager.stop();
        
        assertTrue(robot.getBattery() > 10);
        assertEquals(Robot.Status.READY, robot.getStatus());
        assertEquals(ChargingStation.Status.READY, station.getStatus());
        assertNull(station.getCurrentRobot());
    }
    
    //todo several robots and stations
    @Test
    public void severalRobotsAndStationsTest() throws Exception {
    	
    }
    
    //todo robot wait too long
    @Test
    public void robotWaitingTimeTooLongTest() throws Exception {
    	
    }

    @Test
    public void taskMngStoreTaskEndToEndTest() throws Exception {
        LogManager logger = new LogManager();

        Bin b1 = new Bin("B1", 1, 1);
        StorageManager storageManager = new StorageManager("StorageManager", logger, Arrays.asList(b1));
        RobotManager robotManager = new RobotManager("RobotManager", logger);
        Robot robot = new Robot("R1", "Robot", logger);
        robot.setBattery(100);
        robotManager.addRobot(robot);

        File snapshotDir = Files.createTempDirectory("tasksnapshots").toFile();
        TaskManager taskManager = new TaskManager(
                "TaskManager",
                logger,
                storageManager,
                robotManager,
                snapshotDir
        );

        robotManager.start();
        taskManager.start();

        WarehouseTask storeTask = new WarehouseTask("TS1", TaskType.STORE, "B1", "I1", "BOX");
        taskManager.submit(storeTask);

        waitUntil(
                () -> storeTask.getState() == TaskState.DONE && robot.getStatus() == Status.READY,
                10_000L,
                "STORE task should reach DONE state"
        );

        taskManager.stop();
        robotManager.stop();

        assertEquals(TaskState.DONE, storeTask.getState());
        assertTrue(storageManager.isBinOccupied("B1"), "Bin B1 should be occupied after store");
        Optional<Item> item = storageManager.getItem("B1");
        assertTrue(item.isPresent());
        assertEquals("I1", item.get().getId());
    }

    @Test
    public void taskMngRetriveTaskEndToEndTest() throws Exception {
        LogManager logger = new LogManager();

        Bin b1 = new Bin("B1", 1, 1);
        StorageManager storageManager = new StorageManager("StorageManager", logger, Arrays.asList(b1));
        storageManager.findAndReserveBinForStore("B1", "setup").orElseThrow();
        storageManager.applyAfterRobot(TaskType.STORE, "B1", "setup", new Item("I1", "BOX"));

        assertTrue(storageManager.isBinOccupied("B1"), "B1 must be occupied");

        RobotManager robotManager = new RobotManager("RobotManager", logger);
        Robot robot = new Robot("R1", "Robot", logger);
        robot.setBattery(100);
        robotManager.addRobot(robot);

        File snapshotDir = Files.createTempDirectory("tasksnapshots").toFile();
        TaskManager taskManager = new TaskManager(
                "TaskManager",
                logger,
                storageManager,
                robotManager,
                snapshotDir
        );

        robotManager.start();
        taskManager.start();

        WarehouseTask retrieveTask = new WarehouseTask("TR1", TaskType.RETRIEVE, "B1", "I1", "BOX");
        taskManager.submit(retrieveTask);

        waitUntil(
                () -> retrieveTask.getState() == TaskState.DONE,
                10_000L,
                "RETRIEVE task should reach DONE state"
        );

        taskManager.stop();
        robotManager.stop();

        assertEquals(TaskState.DONE, retrieveTask.getState());
        assertFalse(storageManager.isBinOccupied("B1"), "Bin B1 should be free after retrieve");
        assertTrue(storageManager.getItem("B1").isEmpty(), "No item should remain in B1");
    }
}
