package main.java.robotManagement;

import main.java.Simulation;
import main.java.logging.LogManager;

public class Robot implements Runnable {
	public enum Status {
		READY, ERROR, BUSY, WAITING, CHARGING
	}

	private final LogManager logger;
	private final String systemName;
	private final String id;
	private Status status = Status.READY;
	private int battery = 100;
	private int x = 0;
	private int y = 0;
	private RobotTask currentTask = RobotTask.idle();

	private volatile boolean running = true;

	private final int batteryDrainPerStep = 1;
	private final int stepTime = 50;

	public Robot(String id, String systemName, LogManager logger) {
		this.id = id;
		this.systemName = systemName;
		this.logger = logger;
	}

	public Robot(String id, int startX, int startY, int initialBattery, String systemName, LogManager logger) {
		this.id = id;
		this.x = startX;
		this.y = startY;
		this.battery = Math.max(0, Math.min(100, initialBattery));
		this.systemName = systemName;
		this.logger = logger;
		logger.log(systemName, "Robot initialized " + this.id);
	}

	public String getId() {
		return id;
	}

	public int getBattery() {
		return battery;
	}

	public void setBattery(int battery) {
		this.battery = Math.max(0, Math.min(100, battery));
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		if (this.status != status) {
			this.status = status;
			logger.log(systemName, "Robot " + this.id + " set status " + status);
		}
	}

	public RobotTask getCurrentTask() {
		return currentTask;
	}

	public void setTask(RobotTask task) {
		currentTask = task;
		logger.log(systemName, "Robot " + this.id + " set task " + task.toString());
	}

	public void moveTo(int nx, int ny) {
		setStatus(Status.BUSY);
		logger.log(systemName, "Robot " + this.id + " started moving to " + nx + " " + ny + " from " + x + " " + y);
		int cx = x;
		int cy = y;
		while ((cx != nx || cy != ny) && running) {
			if (cx < nx)
				cx++;
			else if (cx > nx)
				cx--;
			if (cy < ny)
				cy++;
			else if (cy > ny)
				cy--;
			x = cx;
			y = cy;
			drainBattery(batteryDrainPerStep);
			sleepMillis(stepTime);
		}
		logger.log(systemName, "Robot " + this.id + " moved to " + nx + " " + ny);
		setStatus(Status.READY);
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

	public void increaseBatteryPercent(int pct) {
		battery = Math.max(0, Math.min(100, battery + pct));
		logger.log(systemName, "Robot " + this.id + " increased battery on " + pct);
	}

	public void drainBattery(int pct) {
		battery = Math.max(0, battery - pct);
		logger.log(systemName, "Robot " + this.id + " drained battery on " + pct);
	}

	public void onChargeWait() {
		logger.log(systemName, "Robot " + this.id + " waiting for charging");
		setStatus(Status.WAITING);
	}

	public void onChargeStart() {
		logger.log(systemName, "Robot " + this.id + " started charging");
		setStatus(Status.CHARGING);
	}

	public void onChargeComplete() {
		logger.log(systemName, "Robot " + this.id + " completed charging");
		setStatus(Status.READY);
		currentTask = RobotTask.idle();
	}

	public void stop() {
		running = false;
	}

	@Override
	public void run() {
		logger.log(systemName, "Robot thread started " + this.id);
		while (running) {
			RobotTask task = currentTask;
			if (task == null) {
				currentTask = RobotTask.idle();
				continue;
			}
			// todo: loadout
			switch (task.getType()) {
			case MOVE:
				moveTo(task.getTargetX(), task.getTargetY());
				currentTask = RobotTask.idle();
				break;
			case STORE:
				moveTo(task.getTargetX(), task.getTargetY());
				drainBattery(2);
				sleepMillis(5000);
				currentTask = RobotTask.idle();
				break;
			case RETRIEVE:
				moveTo(task.getTargetX(), task.getTargetY());
				drainBattery(2);
				sleepMillis(5000);
				currentTask = RobotTask.idle();
				break;
			case CHARGE:
				if (getX() != task.getTargetX() || getY() != task.getTargetY()) {
					moveTo(task.getTargetX(), task.getTargetY());
				} else {
					onChargeWait();
					sleepMillis(1000);
				}
				break;
			case IDLE:
				sleepMillis(1000);
			default:
				sleepMillis(1000);
				break;
			}
		}
		logger.log(systemName, "Robot thread stopped " + this.id);
	}
}