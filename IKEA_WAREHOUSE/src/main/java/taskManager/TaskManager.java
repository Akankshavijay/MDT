package main.java.taskManager;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import main.java.Manager;
import main.java.exceptionHandler.RobotManagerException;
import main.java.exceptionHandler.StorageException;
import main.java.exceptionHandler.TaskManagerException;
import main.java.logging.LogManager;
import main.java.robotManagement.RobotManager;
import main.java.robotManagement.RobotTask;
import main.java.storageManagement.Bin;
import main.java.storageManagement.Item;
import main.java.storageManagement.StorageManager;

public class TaskManager extends Manager {

	private final StorageManager storageManager;
	private final RobotManager robotManager;

	private final List<WarehouseTask> tasks = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, WarehouseTask> inFlight = new ConcurrentHashMap<>();

	private final File snapshotDir;

	private OutputStream byteStream;
	private Writer charStream;

	public TaskManager(String systemName, LogManager logger, StorageManager storageManager, RobotManager robotManager,
			File snapshotDir) {
		super(systemName, logger);
		this.storageManager = storageManager;
		this.robotManager = robotManager;
		this.snapshotDir = snapshotDir;

		if (this.robotManager != null) {
			this.robotManager.setTaskManager(this);
		}

		logger.log(systemName, "TaskManager initialized. Loaded tasks: " + tasks.size());
	}

//    public void connectStreams(OutputStream byteStream, Writer charStream) {
//        this.byteStream = byteStream;
//        this.charStream = charStream;
//        logger.log(systemName, "Streams connected to TaskManager");
//    }

	public void submit(WarehouseTask task) throws TaskManagerException {
		if (task == null)
			throw new TaskManagerException("Task is null");
		tasks.add(task);
		logger.log(systemName, "Task submitted: " + task);
	}

//    public WarehouseTask addTask(String id, String action, String binId, String itemId, String itemType) {
//        WarehouseTask task = new WarehouseTask(id, action, binId, itemId, itemType);
//
//        try {
//            confirmWithStorage(task);
//            task.setState(TaskState.STANDING_BY);
//            tasks.add(task);
//            logger.log(systemName, "Added task: " + task);
//        } catch (TaskManagerException e) {
//            task.setState(TaskState.ERROR);
//            tasks.add(task);
//            logger.log(systemName, "Added task in ERROR state: " + task + " cause " + e.getMessage());
//        }
//
//        saveSnapshot();
//        return task;
//    }

	public List<WarehouseTask> getTasksSnapshot() {
		synchronized (tasks) {
			return new ArrayList<>(tasks);
		}
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
		if (newIndex < 0)
			newIndex = 0;
		if (newIndex > tasks.size())
			newIndex = tasks.size();
		tasks.add(newIndex, t);
		logger.log(systemName, "Moved task: " + taskId + " to index " + newIndex);
		saveSnapshot();
	}

	public void onRobotTaskCompleted(String robotTaskId, boolean success) {
		WarehouseTask wt = inFlight.remove(robotTaskId);

		if (wt == null) {
			throw new TaskManagerException("Unknown robot task id: " + robotTaskId);
		}

		try {
			if (success) {
				String warehouseTaskId = wt.getId();

				if (wt.getType() == TaskType.STORE) {
					storageManager.applyAfterRobot(TaskType.STORE, wt.getBinId(), warehouseTaskId,
							new Item(wt.getItemId(), wt.getItemType()));
				} else if (wt.getType() == TaskType.RETRIEVE) {
					storageManager.applyAfterRobot(TaskType.RETRIEVE, wt.getBinId(), warehouseTaskId, null);
				}

				wt.setState(TaskState.DONE);
				logger.log(systemName, "Warehouse task completed: " + wt.getId());
			} else {
				if (wt.getType() == TaskType.STORE && wt.getBinId() != null) {
					storageManager.releaseReservation(wt.getBinId(), wt.getId());
				}
				wt.setState(TaskState.ERROR);
				logger.log(systemName, "Warehouse task failed: " + wt.getId());
			}
		} catch (StorageException e) {
			if (wt.getType() == TaskType.STORE && wt.getBinId() != null) {
				try {
					storageManager.releaseReservation(wt.getBinId(), wt.getId());
				} catch (StorageException ignore) {
					logger.log(systemName,
							"Failed to release reservation for task " + wt.getId() + ": " + ignore.getMessage());
				}
			}
			wt.setState(TaskState.ERROR);
			logger.log(systemName, "Storage update failed for task " + wt.getId() + ": " + e.getMessage());
		}
	}

//    public void startTask(String taskId) {
//        WarehouseTask t = findTask(taskId);
//        if (t.getState() != TaskState.STANDING_BY) {
//            throw new TaskManagerException("Task " + taskId + " is not in STANDING_BY state.");
//        }
//
//        t.setState(TaskState.IN_PROGRESS);
//        writeToStreams("[" + LocalDateTime.now() + "] Task " + t.getId() + " IN_PROGRESS\n");
//
//        try {
//            if ("store".equalsIgnoreCase(t.getAction())) {
//                storageManager.requestStore(t.getBinId(),
//                        new Item(t.getItemId(), t.getItemType()));
//            } else if ("retrieve".equalsIgnoreCase(t.getAction())) {
//                storageManager.requestRetrieve(t.getBinId(),
//                        new Item(t.getItemId(), t.getItemType()));
//            } else {
//                throw new TaskManagerException("Unknown action: " + t.getAction());
//            }
//
//            t.setState(TaskState.DONE);
//            logger.log(systemName, "Task DONE: " + t);
//            writeToStreams("[" + LocalDateTime.now() + "] Task " + t.getId() + " DONE\n");
//        } catch (RuntimeException e) {
//            t.setState(TaskState.ERROR);
//            logger.log(systemName, "Task ERROR: " + t + " cause=" + e.getMessage());
//            writeToStreams("[" + LocalDateTime.now() + "] Task " + t.getId() + " ERROR\n");
//            throw new TaskManagerException("Failed to run task " + t.getId(), e);
//        } finally {
//            saveSnapshot();
//        }
//    }

//    public void cancelTask(String taskId) {
//        WarehouseTask t = findTask(taskId);
//        if (t.getState() == TaskState.DONE || t.getState() == TaskState.ERROR) {
//            return; // already finished
//        }
//        t.setState(TaskState.CANCELED);
//        logger.log(systemName, "Task canceled: " + t);
//        writeToStreams("[" + LocalDateTime.now() + "] Task " + t.getId() + " → CANCELED\n");
//        saveSnapshot();
//    }

	private WarehouseTask findTask(String taskId) {
		for (WarehouseTask t : tasks) {
			if (t.getId().equals(taskId)) {
				return t;
			}
		}
		throw new TaskManagerException("Task not found: " + taskId);
	}

//    private void confirmWithStorage(WarehouseTask task) {
//        try {
//            if ("store".equalsIgnoreCase(task.getAction())) {
//                boolean occupied = storageManager.isBinOccupied(task.getBinId());
//                if (occupied) {
//                    throw new TaskManagerException("Bin " + task.getBinId() + " is already occupied.");
//                }
//            } else if ("retrieve".equalsIgnoreCase(task.getAction())) {
//                boolean hasItem = storageManager.getItem(task.getBinId()).isPresent();
//                if (!hasItem) {
//                    throw new TaskManagerException("Bin " + task.getBinId() + " has no item to retrieve.");
//                }
//            } else {
//                throw new TaskManagerException("Unknown action: " + task.getAction());
//            }
//        } catch (RuntimeException e) {
//            throw new TaskManagerException("StorageManager failed to confirm task: " + e.getMessage(), e);
//        }
//    }

	private void saveSnapshot() {
		String baseName = "tasks-" + System.currentTimeMillis();

		File binFile = new File(snapshotDir, baseName + ".bin");
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(binFile))) {
			oos.writeObject(new ArrayList<>(tasks));
		} catch (IOException e) {
			logger.log(systemName, "Cannot write binary snapshot: " + e.getMessage());
		}

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

	@Override
	public void loopOnce() {
		WarehouseTask next = null;
		synchronized (tasks) {
			for (WarehouseTask t : tasks) {
				if (t.getState() == TaskState.STANDING_BY) {
					next = t;
					break;
				}
			}
		}
		if (next == null)
			return;

		try {
			switch (next.getType()) {
			case STORE: {
				Optional<Bin> reserved = storageManager.findAndReserveBinForStore(next.getBinId(), next.getId());
				if (!reserved.isPresent()) {
					return;
				}
				Bin target = reserved.get();
				next.setBinId(target.getId());

				RobotTask rt = TaskAdapter.toRobotTask(next, target);

				next.setState(TaskState.IN_PROGRESS);
				inFlight.put(rt.getId(), next);

				boolean assigned = true;
				try {
					assigned = robotManager.enqueueRobotTask(rt, next);
				} catch (RobotManagerException e) {
					inFlight.remove(rt.getId());
					try {
						storageManager.releaseReservation(target.getId(), next.getId());
					} catch (StorageException se) {
						logger.log(systemName,
								"Failed to release reservation for bin " + target.getId() + ": " + se.getMessage());
					}
					throw e;
				}

				if (!assigned) {
					inFlight.remove(rt.getId());
					try {
						storageManager.releaseReservation(target.getId(), next.getId());
					} catch (StorageException se) {
						logger.log(systemName,
								"Failed to release reservation for bin " + target.getId() + ": " + se.getMessage());
					}
					next.setState(TaskState.ERROR);
				}

				break;
			}

			case RETRIEVE: {
				Optional<Bin> occ = storageManager.findOccupiedBinForRetrieve(next.getBinId());
				if (!occ.isPresent()) {
					return;
				}
				Bin target = occ.get();

				RobotTask rt = TaskAdapter.toRobotTask(next, target);

				next.setState(TaskState.IN_PROGRESS);
				inFlight.put(rt.getId(), next);

				boolean assigned = true;
				try {
					assigned = robotManager.enqueueRobotTask(rt, next);
				} catch (RobotManagerException e) {
					inFlight.remove(rt.getId());
					throw e;
				}

				if (!assigned) {
					inFlight.remove(rt.getId());
					next.setState(TaskState.ERROR);
				}
				break;
			}

			default:
				throw new TaskManagerException("Unsupported task type: " + next.getType());
			}
		} catch (TaskManagerException | RobotManagerException e) {
			next.setState(TaskState.ERROR);
			logger.log(systemName, "Failed to run task " + next.getId() + ": " + e.getMessage());
		}
	}
}