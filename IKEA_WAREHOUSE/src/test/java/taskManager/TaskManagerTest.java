package test.java.taskManager;

import org.junit.jupiter.api.*;

import main.java.exceptionHandler.TaskManagerException;
import main.java.logging.LogManager;
import main.java.robotManagement.RobotManager;
import main.java.robotManagement.RobotTask;
import main.java.storageManagement.Bin;
import main.java.storageManagement.Item;
import main.java.storageManagement.StorageManager;
import main.java.taskManager.*;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TaskManagerTest {

    private StorageManager storageManager;
    private TaskManager taskManager;
    private MockRobotManager robotManager;
    private File snapshotDir;

    private Bin b1, b2;

    @BeforeEach
    public void setUp() throws Exception {
        snapshotDir = Files.createTempDirectory("tasksnapshots").toFile();
        LogManager logger = new LogManager();

        // bins: B1 free, B2 occupied
        b1 = new Bin("B1", 1, 1);
        b2 = new Bin("B2", 2, 2);
        b2.commitStore("setup", new Item("I1", "BOX"));  // pre-occupy

        storageManager = new StorageManager(
                "StorageSys", logger, Arrays.asList(b1, b2)
        );

        robotManager = new MockRobotManager(logger);
        taskManager = new TaskManager(
                "TaskSys", logger, storageManager, robotManager, snapshotDir
        );
    }

    @Test
    public void addStoreTask_accepted_whenBinFree() throws Exception {
        WarehouseTask t = new WarehouseTask("T1", TaskType.STORE, "B1", "I100", "BOX");
        taskManager.submit(t);
        assertEquals(TaskState.STANDING_BY, t.getState());
        assertEquals(1, taskManager.getTasksSnapshot().size());
    }

    @Test
    public void addStoreTask_error_whenBinOccupied() throws Exception {
        WarehouseTask t = new WarehouseTask("T2", TaskType.STORE, "B2", "I101", "BOX");
        taskManager.submit(t);

        // simulate loop tick, bin B2 already occupied, so nothing happens yet
        taskManager.loopOnce();
        assertEquals(TaskState.STANDING_BY, t.getState(), "Still pending until a free bin is available");
    }

    @Test
    public void addRetrieveTask_accepted_whenItemPresent() throws Exception {
        WarehouseTask t = new WarehouseTask("T3", TaskType.RETRIEVE, "B2", "I1", "BOX");
        taskManager.submit(t);
        assertEquals(TaskState.STANDING_BY, t.getState());
    }

    @Test
    public void startTask_store_triggersRobot_and_marksDone() throws Exception {
        WarehouseTask t = new WarehouseTask("T4", TaskType.STORE, "B1", "I200", "BOX");
        taskManager.submit(t);

        // one tick: should reserve B1, call robot, and immediately finish via mock
        taskManager.loopOnce();

        assertTrue(robotManager.lastTaskWasExecuted(), "RobotManager mock must have executed a task");
        assertEquals(TaskState.DONE, t.getState());
        assertEquals(Bin.Status.OCCUPIED, b1.getStatus());
        assertTrue(b1.getItem().isPresent());
        assertEquals("I200", b1.getItem().get().getId());
    }

    @Test
    public void startTask_robotFailure_resultsInErrorAndFreeBin() throws Exception {
        WarehouseTask t = new WarehouseTask("T5", TaskType.STORE, "B1", "I777", "BOX");
        taskManager.submit(t);

        robotManager.setNextSuccess(false); // force failure
        taskManager.loopOnce();

        assertEquals(TaskState.ERROR, t.getState());
        assertEquals(Bin.Status.FREE, b1.getStatus(), "Reservation must be released");
    }

    @Test
    public void retrieveTask_onSuccess_freesBin() throws Exception {
        WarehouseTask t = new WarehouseTask("T6", TaskType.RETRIEVE, "B2", "I1", "BOX");
        taskManager.submit(t);

        taskManager.loopOnce();

        assertEquals(TaskState.DONE, t.getState());
        assertEquals(Bin.Status.FREE, b2.getStatus());
        assertTrue(b2.getItem().isEmpty());
    }

    @Test
    public void retrieveTask_whenBinEmpty_staysPending() throws Exception {
        WarehouseTask t = new WarehouseTask("T7", TaskType.RETRIEVE, "B1", "I999", "BOX");
        taskManager.submit(t);

        taskManager.loopOnce();
        assertEquals(TaskState.STANDING_BY, t.getState());
    }

    @Test
    public void submittingMultipleStoreTasks_respectsReservationIsolation() throws Exception {
        WarehouseTask t1 = new WarehouseTask("T8", TaskType.STORE, null, "I1", "BOX");
        WarehouseTask t2 = new WarehouseTask("T9", TaskType.STORE, null, "I2", "BOX");
        taskManager.submit(t1);
        taskManager.submit(t2);

        // Only one free bin (B1)
        taskManager.loopOnce(); // processes t8
        taskManager.loopOnce(); // t9 can't find free bin

        assertEquals(TaskState.DONE, t1.getState());
        assertEquals(TaskState.STANDING_BY, t2.getState());
        assertEquals(Bin.Status.OCCUPIED, b1.getStatus());
    }

    @Test
    public void startUnknownTask_throwsException() {
        assertThrows(TaskManagerException.class, () -> taskManager.onRobotTaskCompleted("NO_SUCH", true));
    }
    
    // Helpers
    
    private static class MockRobotManager extends RobotManager {
        private TaskManager tm;
        private boolean nextSuccess = true;
        private boolean executed = false;

        public MockRobotManager(LogManager logger) {
            super("MockRobot", logger);
        }

        @Override
        public void setTaskManager(TaskManager tm) {
            super.setTaskManager(tm);
            this.tm = tm;
        }

        @Override
        public boolean enqueueRobotTask(RobotTask rt, WarehouseTask src) {
            executed = true;
            tm.onRobotTaskCompleted(rt.getId(), nextSuccess);
            return true;
        }

        public void setNextSuccess(boolean success) { this.nextSuccess = success; }
        public boolean lastTaskWasExecuted() { boolean e = executed; executed = false; return e; }
        @Override protected void loopOnce() {}
    }
}
