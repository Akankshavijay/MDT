package main.java.chargingManagement;

import java.util.*;
import main.java.robotManagement.Robot;

public class ChargingStation implements Runnable {
    public enum Status { READY, CHARGING, ERROR }

    private final String id;
    private Status status = Status.READY;
    private Robot currentRobot = null;

    private final int chargeRatePercentPerMinute = 1;
    private volatile boolean running = true;

    public ChargingStation(
        String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    public Status getStatus() {
        return status;
    }
    public Robot getCurrentRobot() {
        return currentRobot;
    }

    public boolean tryAssignRobot(Robot robot) {
        Objects.requireNonNull(robot);
        if (status == Status.READY && currentRobot == null) {
        	currentRobot = robot;
            status = Status.CHARGING;
            return true;
        }
        return false;
    }

    public int timeRemainingMinutes() {
        Robot r = currentRobot;
        if (r == null || status != Status.CHARGING)
            return 0;
        int missing = 100 - r.getBattery();
        return Math.max(
            0, missing / chargeRatePercentPerMinute);
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            Robot r = currentRobot;
            if (r == null) {
                status = Status.READY;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }

            try {
                if (r.getStatus() == Robot.Status.WAITING)
                    r.onChargeStart();
                r.increaseBatteryPercent(chargeRatePercentPerMinute);
                Thread.sleep(1000L);

                if (r.getBattery() >= 100) {
                    r.onChargeComplete();
                    currentRobot = null;
                    status = Status.READY;
                }
            } catch (Exception ex) {
                status = Status.ERROR;
            }
        }
    }
}
