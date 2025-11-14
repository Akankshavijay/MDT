package test.java.robotManager;

import main.java.robotManagement.*;
import main.java.exceptionHandler.RobotManagerException;
import main.java.logging.LogManager;

import main.java.chargingManagement.ChargingManager;
import main.java.taskManager.WarehouseTask;
import main.java.taskManager.TaskManager;
import main.java.taskManager.TaskType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class RobotManagerTest {

    // ------------------------------------------------------------
    // Simple TaskManager to capture callbacks
    // ------------------------------------------------------------
    static class TestTaskManager {
        int calls = 0;
        String last = null;

        public void onRobotTaskCompleted(String robotTaskId, boolean success) {
            calls++;
            last = robotTaskId;
        }
    }

    // ------------------------------------------------------------
    // Simple Robot with threading disabled
    // ------------------------------------------------------------
    static class TestRobot extends Robot {

        private RobotTask task = RobotTask.idle();
        private Status status = Status.READY;

        public TestRobot(String id, LogManager logger) {
            // Correct signature (id, battery, x, y, home, logger)
            super(id, 100, 0, 0, "HOME", logger);
        }

        @Override public void run() {} // Disable thread
        @Override public void setTask(RobotTask t) { task = t; }
        @Override public RobotTask getCurrentTask() { return task; }
        @Override public Status getStatus() { return status; }
        @Override public void setStatus(Status s) { status = s; }
    }

    // ------------------------------------------------------------
    // Test setup
    // ------------------------------------------------------------
    private RobotManager manager;
    private ChargingManager chargingManager;
    private TestTaskManager taskManager;
    private LogManager logger;

    @BeforeEach
    void setup() {
        logger = new LogManager();
        manager = new RobotManager("RobotMgr", logger);

        // Real charging manager
        chargingManager = new ChargingManager("ChargerMgr", logger);
        manager.setChargingManager(chargingManager);

        taskManager = new TestTaskManager();
    }

    // ------------------------------------------------------------
    // TESTS
    // ------------------------------------------------------------

    /**
     * SIMPLE + PASSING:
     * Check robot is added and retrievable.
     */
    @Test
    void robotAddedCanBeRetrieved() {
        TestRobot r = new TestRobot("R1", logger);
        manager.addRobot(r);

        assertTrue(manager.getRobot("R1").isPresent());
    }

    /**
     * Check enqueue works when queue has space.
     */
    @Test
    void enqueueSucceedsWhenQueueHasSpace() throws Exception {
        TestRobot r = new TestRobot("R1", logger);
        manager.addRobot(r);

        WarehouseTask wt = new WarehouseTask("T1", TaskType.STORE, "BIN1", "ITEM1", "TYPE");
        RobotTask rt = RobotTask.storeAt(10, 20);

        assertTrue(manager.enqueueRobotTask(rt, wt));
    }

    /**
     * SIMPLE + PASSING:
     * Queue capacity = 1, so second enqueue fails.
     */
    @Test
    void enqueueFailsWhenQueueFull() throws Exception {
        TestRobot r = new TestRobot("R1", logger);
        manager.addRobot(r);

        // First enqueue (fills queue)
        manager.enqueueRobotTask(
                RobotTask.storeAt(1, 1),
                new WarehouseTask("T1", TaskType.STORE, "B1", "I1", "TYPE")
        );

        // Second enqueue must fail
        WarehouseTask w2 = new WarehouseTask("T2", TaskType.STORE, "B2", "I2", "TYPE");

        assertThrows(RobotManagerException.class,
                () -> manager.enqueueRobotTask(RobotTask.storeAt(2, 2), w2));
    }



}
