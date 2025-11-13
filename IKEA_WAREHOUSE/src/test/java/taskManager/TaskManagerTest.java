package test.java.taskManager;

import org.junit.jupiter.api.*;

import main.java.exceptionHandler.RobotManagerException;
import main.java.exceptionHandler.StorageException;
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
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TaskManagerTest {

    private LogManager logger;
    private StorageManager storageManager;
    private MockRobotManager robotManager;
    private TaskManager taskManager;
    private File snapshotDir;

    private Bin b1, b2;

    @BeforeEach
    public void setUp() throws Exception {
        logger = new LogManager();
        snapshotDir = Files.createTempDirectory("tasksnapshots").toFile();

        // B1 free, B2 occupied with I1
        b1 = new Bin("B1", 1, 1);
        b2 = new Bin("B2", 2, 2);

        storageManager = new StorageManager("StorageManager", logger, Arrays.asList(b1, b2));
        storageManager.findAndReserveBinForStore("B2", "setup").orElseThrow(); // reserve B2 and commit
        storageManager.applyAfterRobot(TaskType.STORE, "B2", "setup", new Item("I1", "BOX")); // now B2 has I1
        
        robotManager = new MockRobotManager(logger);
        taskManager = new TaskManager("TaskManager", logger, storageManager, robotManager, snapshotDir);
    }

    @Test
    public void addStoreTask_accepted_whenBinFree() throws Exception {
        WarehouseTask t = new WarehouseTask("T1", TaskType.STORE, "B1", "I100", "BOX");
        taskManager.submit(t);
        assertEquals(TaskState.STANDING_BY, t.getState());
        assertEquals(1, taskManager.getTasksSnapshot().size());
    }

    @Test
    public void addStoreTask_pending_whenBinOccupied() throws Exception {
        // B2 is occupied from setup
        WarehouseTask t = new WarehouseTask("T2", TaskType.STORE, "B2", "I101", "BOX");
        taskManager.submit(t);

        taskManager.loopOnce(); // cannot reserve, remains pending
        assertEquals(TaskState.STANDING_BY, t.getState());
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

        taskManager.loopOnce(); // reserve B1, enqueue and complete via mock

        assertTrue(robotManager.lastTaskWasExecuted(), "RobotManager mock must have executed a task");
        assertEquals(TaskState.DONE, t.getState());
        assertEquals(Bin.Status.OCCUPIED, b1.getStatus());
        assertTrue(b1.getItem().isPresent());
        assertEquals("I200", b1.getItem().get().getId());
    }

    @Test
    public void startTask_robotFailure_resultsInErrorAndReservationReleased() throws Exception {
        WarehouseTask t = new WarehouseTask("T5", TaskType.STORE, "B1", "I777", "BOX");
        taskManager.submit(t);

        robotManager.setNextSuccess(false); // cause failure
        taskManager.loopOnce();

        assertEquals(TaskState.ERROR, t.getState());
        assertEquals(Bin.Status.FREE, b1.getStatus(), "Reservation must be released after failure");
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
    public void multipleStoreTasks_respectReservationIsolation() throws Exception {
        // Only B1 is free, B2 is occupied
        WarehouseTask t1 = new WarehouseTask("T8", TaskType.STORE, null, "I1", "BOX");
        WarehouseTask t2 = new WarehouseTask("T9", TaskType.STORE, null, "I2", "BOX");
        taskManager.submit(t1);
        taskManager.submit(t2);

        taskManager.loopOnce(); // t1 completes and occupies B1
        taskManager.loopOnce(); // t2 cannot reserve any bin

        assertEquals(TaskState.DONE, t1.getState());
        assertEquals(TaskState.STANDING_BY, t2.getState());
        assertEquals(Bin.Status.OCCUPIED, b1.getStatus());
    }

    @Test
    public void storageCommitFailure_marksError_andReleasesReservation() throws Exception {
        // Use a faulty storage manager that throws on commit for STORE
        FaultyStorageManager faulty = new FaultyStorageManager("FaultyStorage", logger, Arrays.asList(b1, b2));
        robotManager = new MockRobotManager(logger);
        taskManager = new TaskManager("TaskSys", logger, faulty, robotManager, snapshotDir);

        WarehouseTask t = new WarehouseTask("T10", TaskType.STORE, "B1", "I555", "BOX");
        taskManager.submit(t);

        taskManager.loopOnce(); // reserve and robot success should lead to commit throws

        assertEquals(TaskState.ERROR, t.getState());
        assertEquals(Bin.Status.FREE, b1.getStatus(), "Reservation should not remain if commit fails");
    }

    @Test
    public void onRobotTaskCompleted_withUnknownId_throws() {
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

        public void setNextSuccess(boolean success) {
            this.nextSuccess = success;
        }

        public boolean lastTaskWasExecuted() {
            boolean out = executed;
            executed = false;
            return out;
        }

        @Override
        public boolean enqueueRobotTask(RobotTask robotTask, WarehouseTask source) throws RobotManagerException {
            executed = true;
            if (tm == null) throw new RobotManagerException("TaskManager not set for mock");
            tm.onRobotTaskCompleted(robotTask.getId(), nextSuccess);
            return true;
        }

        @Override protected void loopOnce() {}
    }

    private static class FaultyStorageManager extends StorageManager {
        public FaultyStorageManager(String name, LogManager log, java.util.Collection<Bin> bins) {
            super(name, log, bins);
        }

        @Override
        public void applyAfterRobot(TaskType type, String binId, String taskId, Item item) throws StorageException {
            if (type == TaskType.STORE) {
                // Simulate failure during commit
                throw new StorageException("Simulated commit failure");
            }
            super.applyAfterRobot(type, binId, taskId, item);
        }
    }
}
