package main.java.robotManagement;

import java.util.*;
import java.util.concurrent.*;
import main.java.chargingManagement.ChargingManager;

public class RobotManager implements Runnable {
    private final Map<String, Robot> robots = new ConcurrentHashMap<>();
    private final BlockingQueue<RobotTask> tasks = new LinkedBlockingQueue<>();
    private ChargingManager chargingManager;

    private volatile boolean running = true;

    private final int lowBatteryThreshold = 20;
    private final int maxAllowedWaitMinutes = 15;

    public RobotManager() {}

    public void setChargingManager(ChargingManager chargingManager) {
          this.chargingManager = chargingManager;
    }

    public void addRobot(Robot robot) {
        robots.put(robot.getId(), robot);
    }
    
    public Optional<Robot> getRobot(String id) {
        return Optional.ofNullable(robots.get(id));
    }

    public void submitTask(RobotTask task) {
        tasks.offer(task);
    }

    private Optional<Robot> findFreeRobot() {
        return robots.values()
            .stream()
            .filter(r
                -> r.getStatus() == Robot.Status.READY
                    && r.getCurrentTask().getType() == RobotTask.Type.IDLE)
            .findFirst();
    }

    private void checkBatteriesAndQueue() {
        for (Robot r : robots.values()) {
            if (r.getStatus() == Robot.Status.CHARGING || r.getStatus() == Robot.Status.WAITING)
                continue;
            int b = r.getBattery();
            if (b <= lowBatteryThreshold) {
                double eta = chargingManager.estimateWaitingTimeMinutes(r);
                if (eta <= maxAllowedWaitMinutes) {
                    if (!chargingManager.isQueued(r))
                        chargingManager.addRobotToQueue(r);
                    //todo: set x y of station
                    r.setTask(RobotTask.charge(10, 15));
                }
            }
        }
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        for (Robot r : robots.values())
            new Thread(r, "Robot-" + r.getId()).start();

        while (running) {
            RobotTask task = tasks.poll();
            if (task != null) {
                Optional<Robot> free = findFreeRobot();
                if (free.isPresent()) {
                    Robot r = free.get();
                    r.setTask(task);
                } else {
                    tasks.offer(task);
                }
            }

            checkBatteriesAndQueue();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
