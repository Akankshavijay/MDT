package main.java.robotManagement;

import java.util.*;
import java.util.concurrent.*;

import main.java.Manager;
import main.java.chargingManagement.ChargingManager;
import main.java.exceptionHandler.RobotManagerException;
import main.java.logging.LogManager;
import main.java.taskManager.TaskManager;
import main.java.taskManager.WarehouseTask;

public class RobotManager extends Manager {
	private final int capacity = 1;
	private final int lowBatteryThreshold = 20;
	private final int maxAllowedWaitMinutes = 15;

	private final Map<String, Robot> robots = new ConcurrentHashMap<>();
	private final BlockingQueue<RobotTask> tasks = new LinkedBlockingQueue<>(capacity);
	private final Map<String, WarehouseTask> taskBridge = new ConcurrentHashMap<>();
	private final Map<String, RobotTask> activeRobotTasks = new ConcurrentHashMap<>();

	private ChargingManager chargingManager;
	private TaskManager taskManager;

	public RobotManager(String systemName, LogManager logger) {
		super(systemName, logger);
		onInitialize();
	}

	public void setChargingManager(ChargingManager chargingManager) {
		this.chargingManager = chargingManager;
	}

	public void setTaskManager(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

	public void addRobot(Robot robot) {
		robots.put(robot.getId(), robot);
		logger.log(systemName, "Added robot " + robot.getId());
	}

	public Optional<Robot> getRobot(String id) {
		return Optional.ofNullable(robots.get(id));
	}

	public boolean enqueueRobotTask(RobotTask robotTask, WarehouseTask source) throws RobotManagerException {
		if (robotTask == null || source == null) {
			throw new RobotManagerException("RobotTask and source WarehouseTask must not be null");
		}
		boolean ok = tasks.offer(robotTask);
		if (!ok) {
			throw new RobotManagerException("Robot task queue is full");
		}
		taskBridge.put(robotTask.getId(), source);
		logger.log(systemName, "Enqueued robot task " + robotTask + " for warehouse task " + source.getId());
		return true;
	}

	private Optional<Robot> findFreeRobot() {
		return robots.values().stream()
				.filter(r -> r.getStatus() == Robot.Status.READY && r.getCurrentTask().getType() == RobotTask.Type.IDLE)
				.findFirst();
	}

	private boolean batteryIsLow(Robot r) {
		if (r.getStatus() == Robot.Status.CHARGING || r.getStatus() == Robot.Status.WAITING)
			return true;
		int b = r.getBattery();
		if (b <= lowBatteryThreshold) {
			double eta = chargingManager.estimateWaitingTimeMinutes(r);
			if (eta <= maxAllowedWaitMinutes) {
				if (!chargingManager.isQueued(r))
					chargingManager.addRobotToQueue(r);
				r.setTask(RobotTask.chargeAt(r.getX(), r.getY()));
				logger.log(systemName, "Set charging task to robot " + r.getId() + " bat: " + r.getBattery());
				return true;
			} else {
				r.setTask(RobotTask.idle());
				r.setStatus(Robot.Status.ERROR);
				logger.log(systemName, "ETA for robot " + r.getId() + " is too big: " + eta);
			}
		}
		return false;
	}

	@Override
	protected void onStart() {
		for (Robot r : robots.values()) {
			new Thread(r, "Robot-" + r.getId()).start();
			logger.log(systemName, "Started robot thread " + r.getId());
		}
	}

	@Override
	protected void loopOnce() {
		// Check for finished robot tasks
		for (Map.Entry<String, RobotTask> entry : new ArrayList<>(activeRobotTasks.entrySet())) {
			String robotId = entry.getKey();
			RobotTask assigned = entry.getValue();
			Robot r = robots.get(robotId);
			if (r == null) {
				activeRobotTasks.remove(robotId);
				continue;
			}

			if (r.getStatus() == Robot.Status.READY && r.getCurrentTask().getType() == RobotTask.Type.IDLE) {

				activeRobotTasks.remove(robotId);
				WarehouseTask src = taskBridge.remove(assigned.getId());

				if (src != null && taskManager != null) {
					taskManager.onRobotTaskCompleted(assigned.getId(), true);
				}
			}
		}

		// Assign new tasks to free robots
		RobotTask task = tasks.poll();
		if (task == null)
			return;

		Optional<Robot> free = findFreeRobot();
		if (!free.isPresent()) {
			tasks.offer(task);
			return;
		}

		Robot robot = free.get();
		try {
			if (!batteryIsLow(robot)) {
				robot.setTask(task);
				activeRobotTasks.put(robot.getId(), task);
				logger.log(systemName, "Set task to robot " + robot.getId());
			} else {
				logger.log(systemName, "Robot " + robot.getId() + " battery low, task " + task.getId() + " deferred");
			}

		} catch (Exception e) {
			logger.log(systemName, "Robot execution error: " + e.getMessage());
			WarehouseTask src = taskBridge.remove(task.getId());

			if (src != null && taskManager != null) {
				taskManager.onRobotTaskCompleted(task.getId(), false);
			}
		}
	}
}