package main.java.chargingManagement;

import java.util.*;
import java.util.concurrent.*;

import main.java.logging.LogManager;
import main.java.robotManagement.Robot;
import main.java.robotManagement.RobotTask;
import main.java.exceptionHandler.*;

import main.java.Manager;

public class ChargingManager extends Manager {

    private final List<ChargingStation> stations = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<Robot> queue = new ConcurrentLinkedQueue<>();

    private double avgChargeMinutes = 10.0;

    public ChargingManager(String systemName, LogManager logger) {
        super(systemName, logger);
        onInitialize();
    }

    public void addStation(ChargingStation station) {
        stations.add(station);
        logger.log(systemName, "Added station " + station.getId());
    }

    public void addRobotToQueue(Robot robot) {
        // Prevent duplicates  
        if (!queue.contains(robot))
            queue.offer(robot);
    }

    public void removeRobotFromQueue(Robot robot) {
        queue.remove(robot);
    }

    public List<ChargingStation> getStations() {
        return stations;
    }

    public boolean isQueued(Robot robot) {
        return queue.contains(robot);
    }

    public int queuePosition(Robot robot) {
        int pos = 0;
        for (Robot r : queue) {
            if (r.equals(robot))
                return pos;
            pos++;
        }
        return -1;
    }

    public double estimateWaitingTimeMinutes(Robot robot) {

        List<ChargingStation> active = stations.stream()
                .filter(s -> s.getStatus() != ChargingStation.Status.ERROR)
                .toList();

        int c = active.size();
        if (c == 0) return Double.POSITIVE_INFINITY;

        double currentWorkload = 0.0;
        for (ChargingStation s : active)
            currentWorkload += s.timeRemainingMinutes();

        int pos = queuePosition(robot);
        int ahead = (pos >= 0 ? pos : queue.size());

        double avg = avgChargeMinutes;
        double totalWorkload = currentWorkload + (ahead * avg);

        return totalWorkload / c;
    }

    public boolean tryAssignRobot(ChargingStation station, Robot robot) {

        if (station.getStatus() == ChargingStation.Status.ERROR)
            throw new RobotCantBeAssignedException(
                    "Robot can't be assigned to charging station, station has status ERROR.");

        // VERY IMPORTANT FIX:
        removeRobotFromQueue(robot);

        // Set robot task
        robot.setTask(RobotTask.chargeAt(station.getX(), station.getY()));

        // Mark robot as CHARGING
        robot.setStatus(Robot.Status.CHARGING);

        // Assign to station
        station.setCurrentRobot(robot);
        station.setStatus(ChargingStation.Status.CHARGING);

        logger.log(systemName, "Assigned robot " + robot.getId() + " to station " + station.getId());
        return true;
    }

    @Override
    protected void onStart() {
        for (ChargingStation c : stations) {
            new Thread(c, "Station-" + c.getId()).start();
            logger.log(systemName, "Started station thread " + c.getId());
        }
    }

    @Override
    protected void loopOnce() {

        for (ChargingStation s : stations) {
            // READY and EMPTY station
            if (s.getCurrentRobot() == null && s.getStatus() == ChargingStation.Status.READY) {

                Robot r = queue.poll();
                if (r != null) {
                    try {
                        tryAssignRobot(s, r);
                    } catch (RobotCantBeAssignedException e) {
                        logger.log(systemName, e.getMessage());
                    }
                }
            }
        }
    }
}
