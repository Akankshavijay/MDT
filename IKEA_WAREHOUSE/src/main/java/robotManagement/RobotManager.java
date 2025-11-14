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

    private final int lowBatteryThreshold = 20;
    private final int maxAllowedWaitMinutes = 15;

    private final Map<String, Robot> robots = new ConcurrentHashMap<>();

    private final BlockingQueue<RobotTask> tasks = new LinkedBlockingQueue<>();

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
    
    public ChargingManager getChargingManager() {
    	return this.chargingManager;
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
    
    public Map<String, Robot> getRobots() {
    	return robots;
    }

    public boolean enqueueRobotTask(RobotTask robotTask, WarehouseTask source) throws RobotManagerException {
        if (robotTask == null || source == null) {
            throw new RobotManagerException("RobotTask and source WarehouseTask must not be null");
        }

        try {
            tasks.put(robotTask);  // FIX: always enqueue safely
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RobotManagerException("Failed to enqueue robot task");
        }

        taskBridge.put(robotTask.getId(), source);
        logger.log(systemName, "Enqueued robot task " + robotTask + " for warehouse task " + source.getId());

        return true;
    }

    private Optional<Robot> findFreeRobot() {
        return robots.values().stream()
                .filter(r ->
                        r.getStatus() == Robot.Status.READY &&
                        r.getCurrentTask().getType() == RobotTask.Type.IDLE &&
                        r.getBattery() > lowBatteryThreshold)
                .findFirst();
    }

    private boolean batteryIsLow(Robot r) {

        if (r.getStatus() == Robot.Status.CHARGING)
            return true;

        if (chargingManager.isQueued(r))
            return true;

        if (r.getBattery() <= lowBatteryThreshold) {

            chargingManager.addRobotToQueue(r);

            r.setStatus(Robot.Status.WAITING);

            logger.log(systemName,
                    "Robot " + r.getId() + " LOW battery (" + r.getBattery()
                            + "%). Queued for charging.");

            return true;
        }

        return false;
    }


    @Override
    protected void onStart() {
        for (Robot r : robots.values()) {
            r.start();
            logger.log(systemName, "Started robot thread " + r.getId());
        }
    }

    @Override
    protected void loopOnce() {
    	
        for (Robot r : robots.values()) {
            if (r.getBattery() <= 20 &&
                r.getStatus() == Robot.Status.READY &&
                !chargingManager.isQueued(r)) {

                chargingManager.addRobotToQueue(r);
                r.setTask(RobotTask.chargeAt(
                    chargingManager.getStations().get(0).getX(),
                    chargingManager.getStations().get(0).getY()
                ));
            }
        }
        
        for (Map.Entry<String, RobotTask> entry : new ArrayList<>(activeRobotTasks.entrySet())) {

            String robotId = entry.getKey();
            RobotTask assigned = entry.getValue();
            Robot r = robots.get(robotId);

            if (r == null) {
                activeRobotTasks.remove(robotId);
                continue;
            }

            if (r.getStatus() == Robot.Status.READY &&
                r.getCurrentTask().getType() == RobotTask.Type.IDLE) {

                activeRobotTasks.remove(robotId);

                WarehouseTask src = taskBridge.remove(assigned.getId());
                if (src != null && taskManager != null) {
                    taskManager.onRobotTaskCompleted(assigned.getId(), true);
                }
            }
        }

        RobotTask task = tasks.poll();
        if (task == null)
            return;

        Optional<Robot> free = findFreeRobot();
        if (!free.isPresent()) {

            try {
                tasks.put(task);
            } catch (InterruptedException ignored) {}
            return;
        }

        Robot robot = free.get();

        try {
            if (batteryIsLow(robot)) {
                tasks.put(task);
                return;
            }

            robot.setTask(task);
            activeRobotTasks.put(robot.getId(), task);

            logger.log(systemName, "Assigned task " + task.getId() + " to robot " + robot.getId());

        } catch (Exception e) {

            logger.log(systemName, "Robot execution error: " + e.getMessage());

            WarehouseTask src = taskBridge.remove(task.getId());
            if (src != null && taskManager != null) {
                taskManager.onRobotTaskCompleted(task.getId(), false);
            }
        }
    }
}
