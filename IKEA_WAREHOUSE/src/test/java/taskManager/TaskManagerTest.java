package test.java.taskManager;

import org.junit.jupiter.api.*;

import main.java.communication.WarehouseMessage;
import main.java.exceptionHandler.TaskManagerException;
import main.java.exceptionHandler.WarehouseException;
import main.java.logging.LogManager;
import main.java.storageManagement.Bin;
import main.java.storageManagement.Item;
import main.java.storageManagement.StorageManager;
import main.java.taskManager.TaskManager;
import main.java.taskManager.TaskState;
import main.java.taskManager.WarehouseTask;
import main.java.warehouseMap.WarehouseMap;

import java.io.*;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskManagerTest {

    private StorageManager storageManager;
    private TaskManager taskManager;

    private ByteArrayOutputStream storageByteOut;
    private ObjectOutputStream storageObjOut;

    private ByteArrayOutputStream tmByteOut;
    private StringWriter tmCharOut;

    private File snapshotDir;

    @BeforeEach
    public void setUp() throws Exception {
        // temp directory for snapshots
        snapshotDir = Files.createTempDirectory("tasksnapshots").toFile();

        LogManager logManager = new LogManager();
        WarehouseException handler = new WarehouseException();
        // real map: 1 entry, 1 exit, 1 charging
        WarehouseMap warehouseMap = new WarehouseMap(1, 1, 1);

        // StorageManager needs stream to send WarehouseMessage
        storageByteOut = new ByteArrayOutputStream();
        storageObjOut = new ObjectOutputStream(storageByteOut);

        storageManager = new StorageManager(
                "StorageSys",
                logManager,
                handler,
                warehouseMap,
                10
        );
        storageManager.connectStream(storageObjOut);

        // create real bins consistent with your Bin class
        // B1 – empty
        Bin b1 = new Bin("B1", 1);
        // B2 – occupied with I1
        Bin b2 = new Bin("B2", 1);
        b2.setItem(new Item("I1", "BOX"));

        storageManager.addBin(b1);
        storageManager.addBin(b2);

        // TaskManager under test
        taskManager = new TaskManager(
                "TaskSys",
                storageManager,
                logManager,
                snapshotDir
        );

        // connect simulated data-exchange streams
        tmByteOut = new ByteArrayOutputStream();
        tmCharOut = new StringWriter();
        taskManager.connectStreams(tmByteOut, tmCharOut);
    }

    @Test
    public void addStoreTask_accepted_whenBinFree() {
        WarehouseTask t = taskManager.addTask("T1", "store", "B1", "I100", "BOX");
        assertEquals(TaskState.STANDING_BY, t.getState());
        assertEquals(1, taskManager.getTasks().size());
        assertSnapshotsCreated();
    }

    @Test
    public void addStoreTask_error_whenBinOccupied() {
        // B2 in setup is occupied with I1
        WarehouseTask t = taskManager.addTask("T2", "store", "B2", "I101", "BOX");
        assertEquals(TaskState.ERROR, t.getState());
        assertEquals(1, taskManager.getTasks().size());
        assertSnapshotsCreated();
    }

    @Test
    public void addRetrieveTask_accepted_whenItemPresent() {
        WarehouseTask t = taskManager.addTask("T3", "retrieve", "B2", "I1", "BOX");
        assertEquals(TaskState.STANDING_BY, t.getState());
        assertEquals(1, taskManager.getTasks().size());
        assertSnapshotsCreated();
    }

    @Test
    public void startTask_store_sendsMessage_and_marksDone() throws Exception {
        WarehouseTask t = taskManager.addTask("T4", "store", "B1", "I200", "BOX");
        assertEquals(TaskState.STANDING_BY, t.getState());

        taskManager.startTask("T4");

        assertEquals(TaskState.DONE, t.getState());
        assertSnapshotsCreated();

        // read what StorageManager sent through its ObjectOutputStream
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(storageByteOut.toByteArray())
        );
        Object obj = ois.readObject();
        assertTrue(obj instanceof WarehouseMessage);
        WarehouseMessage msg = (WarehouseMessage) obj;
        assertEquals("store", msg.getAction());
        assertEquals("B1", msg.getBinId());
        assertEquals("I200", msg.getItemId());

        // TaskManager also wrote to char stream
        String charLog = tmCharOut.toString();
        assertTrue(charLog.contains("DONE"));
    }

    @Test
    public void moveTask_changesOrder() {
        taskManager.addTask("T1", "store", "B1", "I10", "BOX");
        taskManager.addTask("T2", "store", "B1", "I11", "BOX");
        taskManager.addTask("T3", "store", "B1", "I12", "BOX");

        // now order is: T1, T2, T3
        taskManager.moveTask("T1", 2);

        List<WarehouseTask> tasks = taskManager.getTasks();
        assertEquals("T2", tasks.get(0).getId());
        assertEquals("T3", tasks.get(1).getId());
        assertEquals("T1", tasks.get(2).getId());
    }

    @Test
    public void cancelTask_setsCanceled() {
        WarehouseTask t = taskManager.addTask("T5", "store", "B1", "I300", "BOX");
        taskManager.cancelTask("T5");
        assertEquals(TaskState.CANCELED, t.getState());
    }

    @Test
    public void startUnknownTask_throws() {
        assertThrows(TaskManagerException.class, () -> taskManager.startTask("NO_SUCH"));
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private void assertSnapshotsCreated() {
        File[] files = snapshotDir.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0, "Snapshot directory should contain at least one file");
    }
}
