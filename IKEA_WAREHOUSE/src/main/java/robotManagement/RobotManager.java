package main.java.robotManagement;

import java.util.*;
import java.util.concurrent.*;

import main.java.Manager;
import main.java.chargingManagement.ChargingManager;
import main.java.logging.LogManager;

public class RobotManager extends Manager {
    private final Map<String, Robot> robots = new ConcurrentHashMap<>();
    private final BlockingQueue<RobotTask> tasks = new LinkedBlockingQueue<>();
    private ChargingManager chargingManager;

    private final int lowBatteryThreshold = 20;
    private final int maxAllowedWaitMinutes = 15;

    public RobotManager(String systemName, 
    		LogManager logger) {
    	super(systemName, logger);
    	onInitialize();
	}

    public void setChargingManager(ChargingManager chargingManager) {
          this.chargingManager = chargingManager;
    }

    public void addRobot(Robot robot) {
        robots.put(robot.getId(), robot);
        logger.log(systemName, "Added robot" + robot.getId());
    }
    
    public Optional<Robot> getRobot(String id) {
        return Optional.ofNullable(robots.get(id));
    }

    public boolean submitTask(RobotTask task) {
    	logger.log(systemName, "Submited task" + task.toString());
        return tasks.offer(task);
    }

    private Optional<Robot> findFreeRobot() {
        return robots.values()
            .stream()
            .filter(r
                -> r.getStatus() == Robot.Status.READY
                    && r.getCurrentTask().getType() == RobotTask.Type.IDLE)
            .findFirst();
    }

    private boolean batteryIsLow(Robot r) {
    	if (r.getStatus() == Robot.Status.CHARGING || r.getStatus() == Robot.Status.WAITING)
            return true;
        int b = r.getBattery();
        if (b <= lowBatteryThreshold) {
            double eta = chargingManager.estimateWaitingTimeMinutes(r);
            if (eta <= maxAllowedWaitMinutes) {
                if (!chargingManager.isQueued(r)) chargingManager.addRobotToQueue(r);
                r.setTask(RobotTask.charge(r.getX(), r.getY()));
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
            logger.log(systemName, "Started robot thread" + r.getId());
        }
    }
    
    @Override
    protected void loopOnce() {
        Optional<Robot> free = findFreeRobot();
        if (free.isPresent()) {
            Robot r = free.get();
            if (!batteryIsLow(r)) {
                RobotTask task = tasks.poll();
                if (task != null) {
                    r.setTask(task);
                    logger.log(systemName, "Set task to robot " + r.getId());
                }
            }
        }
    }
}
