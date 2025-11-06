package main.java.chargingManagement;

import java.util.*;
import java.util.concurrent.*;
import main.java.robotManagement.Robot;

public class ChargingManager implements Runnable {
    private final List<ChargingStation> stations = new CopyOnWriteArrayList<>();
    private final ConcurrentLinkedQueue<Robot> queue = new ConcurrentLinkedQueue<>();

    private double avgChargeMinutes = 10.0;

    private volatile boolean running = true;

    public ChargingManager() {}

    public void addStation(ChargingStation station) {
        stations.add(station);
    }

    public void addRobotToQueue(Robot robot) {
        if (!queue.contains(robot))
            queue.offer(robot);
    }

    public void removeRobotFromQueue(Robot robot) {
        queue.remove(robot);
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
        List<ChargingStation> active =
            stations.stream()
                .filter(s -> s.getStatus() != ChargingStation.Status.ERROR)
                .toList();
        int c = active.size();
        if (c == 0)
            return Double.POSITIVE_INFINITY;

        double currentWorkload = 0.0;
        for (ChargingStation s : active)
            currentWorkload += s.timeRemainingMinutes();

        int ahead;
        int pos = queuePosition(robot);
        if (pos >= 0) {
            ahead = pos;
        } else {
            ahead = queue.size();
        }

        double avg = avgChargeMinutes;
        double totalWorkload = currentWorkload + (ahead * avg);
        return totalWorkload / c;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
    	for (ChargingStation c : stations)
            new Thread(c, "Station-" + c.getId()).start();
    	
        while (running) {
            for (ChargingStation s : stations) {
                if (s.getCurrentRobot() == null
                    && s.getStatus() == ChargingStation.Status.READY) {
                	//todo: assign robot to station
                }
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}