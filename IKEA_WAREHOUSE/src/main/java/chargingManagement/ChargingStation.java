package main.java.chargingManagement;

import java.util.*;

import main.java.Simulation;
import main.java.logging.LogManager;
import main.java.robotManagement.Robot;

public class ChargingStation implements Runnable {
	public enum Status {
		READY, CHARGING, ERROR
	}

	private final LogManager logger;
	private final String systemName;

	private final String id;
	private int x = 0;
	private int y = 0;
	private Status status = Status.READY;
	private Robot currentRobot = null;

	private final int chargeRatePercentPerSecond = 20;
	private volatile boolean running = true;

	public ChargingStation(String id, String systemName, LogManager logger) {
		this.systemName = systemName;
		this.logger = logger;
		this.id = id;
	}

	public ChargingStation(String id, int x, int y, String systemName, LogManager logger) {
		this.systemName = systemName;
		this.logger = logger;
		this.id = id;
		this.x = x;
		this.y = y;
	}

	public String getId() {
		return id;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public Status getStatus() {
		return this.status;
	}

	public void setStatus(Status s) {
		this.status = s;
		logger.log(systemName, "Station " + this.id + " set status " + s.toString());
	}

	public Robot getCurrentRobot() {
		return this.currentRobot;
	}

	public void setCurrentRobot(Robot r) {
		this.currentRobot = r;
		logger.log(systemName, "Station " + this.id + " set robot " + r.getId());
	}

	public int timeRemainingMinutes() {
		Robot r = currentRobot;
		if (r == null || status != Status.CHARGING)
			return 0;
		int missing = 100 - r.getBattery();
		return Math.max(0, missing / (chargeRatePercentPerSecond * 60));
	}

	public void stop() {
		running = false;
		logger.log(systemName, "Station thread stopped " + this.id);
	}

	@Override
	public void run() {
		logger.log(systemName, "Station thread started " + this.id);
		while (running) {
			Robot r = currentRobot;
			if (r == null) {
				sleepMillis(50);
				continue;
			}

			try {
				if (r.getStatus() == Robot.Status.WAITING)
					r.onChargeStart();

				if (r.getStatus() == Robot.Status.CHARGING) {
					r.increaseBatteryPercent(chargeRatePercentPerSecond);
					sleepMinutes(1);
				}

				if (r.getBattery() >= 99) {
					r.onChargeComplete();
					currentRobot = null;
					setStatus(Status.READY);
				}
			} catch (Exception ex) {
				setStatus(Status.ERROR);
			}
		}

		logger.log(systemName, "Station thread stopped" + this.id);
	}
	
	public void sleepMinutes(long minutes) {
	    long baseMillis = minutes * 60_000L;
	    int speed = Simulation.getSimulationSpeed();   // 1, 2, 4 or 8
	    if (speed <= 0) speed = 1;

	    long scaledMillis = baseMillis / speed;
	    if (scaledMillis < 1L) {
	        scaledMillis = 1L;
	    }

	    try {
	        Thread.sleep(scaledMillis);
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	    }
	}

	public void sleepMillis(long ms) {
	    int speed = Simulation.getSimulationSpeed();   // 1, 2, 4 or 8
	    if (speed <= 0) speed = 1;

	    long scaledMillis = ms / speed;
	    if (scaledMillis < 1L) {
	        scaledMillis = 1L;
	    }

	    try {
	        Thread.sleep(scaledMillis);
	    } catch (InterruptedException e) {
	        Thread.currentThread().interrupt();
	    }
	}
}
