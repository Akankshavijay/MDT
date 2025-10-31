package main.java.TaskManager;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

import main.java.Communication.WarehouseMessage;
import main.java.Logging.LogManager;
import main.java.StorageManagement.Item;
import main.java.StorageManagement.StorageManager;
import main.java.ExceptionHandler.TaskManagerException;

public class TaskManager {

    private final String systemName;
    private final StorageManager storageManager;
    private final LogManager logger;
    private final File snapshotDir;

    private final List<WarehouseTask> tasks = new ArrayList<>();

    private OutputStream byteStream;
    private Writer charStream;

    public TaskManager(String systemName,
                       StorageManager storageManager,
                       LogManager logger,
                       File snapshotDir) {
        this.systemName = systemName;
        this.storageManager = storageManager;
        this.logger = logger;
        this.snapshotDir = snapshotDir;

        if (!snapshotDir.exists()) {
            snapshotDir.mkdirs();
        }

        loadLastSnapshot();
        logger.log(systemName, "TaskManager initialized. Loaded tasks: " + tasks.size());
    }

    public void connectStreams(OutputStream byteStream, Writer charStream) {
        this.byteStream = byteStream;
        this.charStream = charStream;
        logger.log(systemName, "Streams connected to TaskManager");
    }


    public WarehouseTask addTask(String id, String action, String binId, String itemId, String itemType) {
        WarehouseTask task = new WarehouseTask(id, action, binId, itemId, itemType);

        try {
            confirmWithStorage(task);
            task.setState(TaskState.STANDING_BY);
            tasks.add(task);
            logger.log(systemName, "Added task: " + task);
        } catch (TaskManagerException e) {
            task.setState(TaskState.ERROR);
            tasks.add(task);
            logger.log(systemName, "Added task in ERROR state: " + task + " cause " + e.getMessage());
        }

        saveSnapshot();
        return task;
    }

    public void removeTask(String taskId) {
        WarehouseTask t = findTask(taskId);
        tasks.remove(t);
        logger.log(systemName, "Removed task: " + taskId);
        saveSnapshot();
    }

    public void moveTask(String taskId, int newIndex) {
        WarehouseTask t = findTask(taskId);
        tasks.remove(t);
        if (newIndex < 0) newIndex = 0;
        if (newIndex > tasks.size()) newIndex = tasks.size();
        tasks.add(newIndex, t);
        logger.log(systemName, "Moved task: " + taskId + " to index " + newIndex);
        saveSnapshot();
    }

    public void startTask(String taskId) {
        WarehouseTask t = findTask(taskId);
        if (t.getState() != TaskState.STANDING_BY) {
            throw new TaskManagerException("Task " + taskId + " is not in STANDING_BY state.");
        }

        t.setState(TaskState.IN_PROGRESS);
        writeToStreams("[" + LocalDateTime.now() + "] Task " + t.getId() + " IN_PROGRESS\n");

        try {
            if ("store".equalsIgnoreCase(t.getAction())) {
                storageManager.requestStore(t.getBinId(),
                        new Item(t.getItemId(), t.getItemType()));
            } else if ("retrieve".equalsIgnoreCase(t.getAction())) {
                storageManager.requestRetrieve(t.getBinId(),
                        new Item(t.getItemId(), t.getItemType()));
            } else {
                throw new TaskManagerException("Unknown action: " + t.getAction());
            }

            t.setState(TaskState.DONE);
            logger.log(systemName, "Task DONE: " + t);
            writeToStreams("[" + LocalDateTime.now() + "] Task " + t.getId() + " DONE\n");
        } catch (RuntimeException e) {
            t.setState(TaskState.ERROR);
            logger.log(systemName, "Task ERROR: " + t + " cause=" + e.getMessage());
            writeToStreams("[" + LocalDateTime.now() + "] Task " + t.getId() + " ERROR\n");
            throw new TaskManagerException("Failed to run task " + t.getId(), e);
        } finally {
            saveSnapshot();
        }
    }

    public void cancelTask(String taskId) {
        WarehouseTask t = findTask(taskId);
        if (t.getState() == TaskState.DONE || t.getState() == TaskState.ERROR) {
            return; // already finished
        }
        t.setState(TaskState.CANCELED);
        logger.log(systemName, "Task canceled: " + t);
        writeToStreams("[" + LocalDateTime.now() + "] Task " + t.getId() + " → CANCELED\n");
        saveSnapshot();
    }

    public List<WarehouseTask> getTasks() {
        return Collections.unmodifiableList(tasks);
    }

    private WarehouseTask findTask(String taskId) {
        for (WarehouseTask t : tasks) {
            if (t.getId().equals(taskId)) {
                return t;
            }
        }
        throw new TaskManagerException("Task not found: " + taskId);
    }

    private void confirmWithStorage(WarehouseTask task) {
        try {
            if ("store".equalsIgnoreCase(task.getAction())) {
                boolean occupied = storageManager.isBinOccupied(task.getBinId());
                if (occupied) {
                    throw new TaskManagerException("Bin " + task.getBinId() + " is already occupied.");
                }
            } else if ("retrieve".equalsIgnoreCase(task.getAction())) {
                boolean hasItem = storageManager.getItem(task.getBinId()).isPresent();
                if (!hasItem) {
                    throw new TaskManagerException("Bin " + task.getBinId() + " has no item to retrieve.");
                }
            } else {
                throw new TaskManagerException("Unknown action: " + task.getAction());
            }
        } catch (RuntimeException e) {
            throw new TaskManagerException("StorageManager failed to confirm task: " + e.getMessage(), e);
        }
    }

    private void saveSnapshot() {
        String baseName = "tasks-" + System.currentTimeMillis();

        // binary snapshot
        File binFile = new File(snapshotDir, baseName + ".bin");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(binFile))) {
            oos.writeObject(new ArrayList<>(tasks));
        } catch (IOException e) {
            logger.log(systemName, "Cannot write binary snapshot: " + e.getMessage());
        }

        // text snapshot
        File txtFile = new File(snapshotDir, baseName + ".txt");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(txtFile), "UTF-8")) {
            for (WarehouseTask t : tasks) {
                w.write(t.toString());
                w.write("\n");
            }
        } catch (IOException e) {
            logger.log(systemName, "Cannot write text snapshot: " + e.getMessage());
        }
    }

    private void loadLastSnapshot() {
        File[] files = snapshotDir.listFiles((dir, name) -> name.endsWith(".bin"));
        if (files == null || files.length == 0) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        File latest = files[files.length - 1];
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(latest))) {
            List<WarehouseTask> loaded = (List<WarehouseTask>) ois.readObject();
            tasks.clear();
            tasks.addAll(loaded);
        } catch (Exception e) {
            logger.log(systemName, "Cannot load snapshot: " + e.getMessage());
        }
    }

    private void writeToStreams(String msg) {
        if (byteStream != null) {
            try {
                byteStream.write(msg.getBytes("UTF-8"));
                byteStream.flush();
            } catch (IOException e) {
                logger.log(systemName, "Error writing to byte stream: " + e.getMessage());
            }
        }

        if (charStream != null) {
            try {
                charStream.write(msg);
                charStream.flush();
            } catch (IOException e) {
                logger.log(systemName, "Error writing to char stream: " + e.getMessage());
            }
        }
    }
}

