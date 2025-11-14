package main.java;

import main.java.logging.LogManager;

public abstract class Manager implements Runnable {
	protected final String systemName;
	protected final LogManager logger;
	protected volatile boolean running = false;
	private long loopSleepMillis = 50L;
	protected Thread thread;

	protected Manager(String systemName, LogManager logger) {
		this.systemName = systemName;
		this.logger = logger;
	}

	public final synchronized void start() {
		if (thread != null && thread.isAlive())
			return;
		running = true;
		thread = new Thread(this, systemName);
		thread.start();
	}

	public final synchronized void stop() {
		running = false;
		logger.log(systemName, systemName + " got stop signal");
		if (thread != null)
			thread.interrupt();
	}

	public boolean isRunning() {
		return running;
	}

	public void setLoopSleepMillis(long ms) {
		loopSleepMillis = ms;
	}

	protected void onInitialize() {
		logger.log(systemName, systemName + " initialized");
	}

	protected void onStart() {
	}

	protected void onStop() {
		logger.log(systemName, systemName + " stopped");
	}

	protected void onError(Throwable t) {
		logger.log(systemName, systemName + " error: " + t.getMessage());
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

	@Override
	public final void run() {
		onStart();
		while (running) {
			try {
				loopOnce();
			} catch (InterruptedException ie) {
				Thread.currentThread().interrupt();
				break;
			} catch (Throwable t) {
				onError(t);
			}
			if (loopSleepMillis > 0) {
				try {
					Thread.sleep(loopSleepMillis);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		onStop();
	}

	protected abstract void loopOnce() throws Exception;
}
