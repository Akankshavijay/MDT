package main.java.chargingManagement;

import main.java.Entities;
import main.java.Simulation;
import main.java.Sleeper;
import main.java.logging.LogManager;
import main.java.robotManagement.Robot;

public class ChargingStation implements Runnable, Entities, Sleeper {

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
        this.id = id;
        this.systemName = systemName;
        this.logger = logger;
    }

    public ChargingStation(String id, int x, int y, String systemName, LogManager logger) {
        this(id, systemName, logger);
        this.x = x;
        this.y = y;
    }

    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public Status getStatus() { return this.status; }
    public Robot getCurrentRobot() { return this.currentRobot; }

    public void setStatus(Status s) {
        this.status = s;
        logger.log(systemName, "Station " + this.id + " set status " + s);
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

        double secondsNeeded = (double) missing / chargeRatePercentPerSecond;
        return (int) Math.ceil(secondsNeeded / 60.0);
    }
    
    @Override
    public void start() {
    	new Thread(this, "Station-" + this.id).start();
    }

    @Override
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

                if (r.getStatus() != Robot.Status.CHARGING) {
                    r.onChargeStart();
                    r.setStatus(Robot.Status.CHARGING);
                }

                r.increaseBatteryPercent(chargeRatePercentPerSecond);
                sleepMillis(1000);

                if (r.getBattery() >= 100) {

                    r.setBattery(100);
                    r.onChargeComplete();
                    logger.log(systemName, "Robot " + r.getId() + " fully charged.");

                    currentRobot = null;
                    setStatus(Status.READY);
                }

            } catch (Exception ex) {
                setStatus(Status.ERROR);
            }
        }

        logger.log(systemName, "Station thread stopped " + this.id);
    }

    @Override
    public void sleepMillis(long ms) {
        int speed = Simulation.getSimulationSpeed();
        if (speed <= 0) speed = 1;

        long scaledMillis = ms / speed;
        if (scaledMillis < 1L) scaledMillis = 1L;

        try { Thread.sleep(scaledMillis); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

	@Override
	public void sleepMinutes(long minutes) {
		
		
	}
}
