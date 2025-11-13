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
        if (thread != null && thread.isAlive()) return;
        running = true;
        thread = new Thread(this, systemName);
        thread.start();
    }

    public final synchronized void stop() {
        running = false;
        logger.log(systemName, " got stop signal");
        if (thread != null) thread.interrupt();
    }

    public boolean isRunning() { 
    	return running; 
    }
    
    public void setLoopSleepMillis(long ms) { 
    	loopSleepMillis = ms; 
    }

    protected void onInitialize() { 
    	logger.log(systemName, " initialized");
    }
    
    protected void onStart() {}
    
    protected void onStop()  { 
    	logger.log(systemName, " stopped");
    }
    
    protected void onError(Throwable t) { 
    	logger.log(systemName, " error: " + t.getMessage());
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
                try { Thread.sleep(loopSleepMillis); }
                catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }
        onStop();
    }
    
    protected abstract void loopOnce() throws Exception;
}
