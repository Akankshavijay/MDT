package TaskManager;

import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

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
        snapshotDir = Files.createTempDirectory("tasksnapshots").toFile();

        DummyLogManager log = new DummyLogManager();
        DummyWarehouseException handler = new DummyWarehouseException();
        DummyWarehouseMap map = new DummyWarehouseMap();

        storageByteOut = new ByteArrayOutputStream();
        storageObjOut = new ObjectOutputStream(storageByteOut);

        storageManager = new StorageManager(
                "StorageSys",
                log,
                handler,
                map,
                10
        );
        storageManager.connectStream(storageObjOut);

        DummyBin emptyBin = new DummyBin("B1", false, null);
        DummyBin fullBin = new DummyBin("B2", true, new Item("I1", "BOX"));

        storageManager.addBin(emptyBin);
        storageManager.addBin(fullBin);

        taskManager = new TaskManager(
                "TaskSys",
                storageManager,
                log,
                snapshotDir
        );

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

        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(storageByteOut.toByteArray())
        );
        Object obj = ois.readObject();
        assertTrue(obj instanceof WarehouseMessage);
        WarehouseMessage msg = (WarehouseMessage) obj;
        assertEquals("store", msg.getAction());
        assertEquals("B1", msg.getBinId());
        assertEquals("I200", msg.getItemId());

        String charLog = tmCharOut.toString();
        assertTrue(charLog.contains("DONE"));
    }

    @Test
    public void moveTask_changesOrder() {
        WarehouseTask t1 = taskManager.addTask("T1", "store", "B1", "I10", "BOX");
        WarehouseTask t2 = taskManager.addTask("T2", "store", "B1", "I11", "BOX");
        WarehouseTask t3 = taskManager.addTask("T3", "store", "B1", "I12", "BOX");

        taskManager.moveTask("T1", 2); // move first to index 2

        List<WarehouseTask> list = taskManager.getTasks();
        assertEquals("T2", list.get(0).getId());
        assertEquals("T3", list.get(1).getId());
        assertEquals("T1", list.get(2).getId());
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

    private void assertSnapshotsCreated() {
        File[] files = snapshotDir.listFiles();
        assertNotNull(files);
        assertTrue(files.length > 0, "Snapshot directory should contain files");
    }

    private static class DummyLogManager {
        public void log(String system, String message) {
            System.out.println("[" + system + "] " + message);
        }
    }

    private static class DummyWarehouseException {
        public void handleWarehouseOperation(String system, Runnable r) {
            r.run();
        }
    }

    private static class DummyWarehouseMap {
        private final Map<String, int[]> entries = new HashMap<>();
        private final Map<String, int[]> exits = new HashMap<>();

        public DummyWarehouseMap() {
            entries.put("E1", new int[]{0, 0});
            exits.put("X1", new int[]{10, 10});
        }

        public Map<String, int[]> getEntryPoints() {
            return entries;
        }

        public Map<String, int[]> getExitPoints() {
            return exits;
        }
    }

    private static class DummyBin extends Bin {
        private final String id;
        private boolean occupied;
        private Item item;

        public DummyBin(String id, boolean occupied, Item item) {
            this.id = id;
            this.occupied = occupied;
            this.item = item;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public boolean isOccupied() {
            return occupied;
        }

        @Override
        public Optional<Item> getItem() {
            return Optional.ofNullable(item);
        }

        @Override
        public void computeDistances(List<int[]> entryPoints, List<int[]> exitPoints) {
        }
    }
}
