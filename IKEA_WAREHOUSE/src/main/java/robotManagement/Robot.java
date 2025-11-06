package main.java.robotManagement;

public class Robot implements Runnable {
    public enum Status { READY, ERROR, BUSY, WAITING, CHARGING }

    private final String id;
    private Status status = Status.READY;
    private int battery = 100;
    private int x = 0;
    private int y = 0;
    private RobotTask currentTask = RobotTask.idle();

    private volatile boolean running = true;

    private final int batteryDrainPerStep = 1;
    private final int stepTime = 50;

    public Robot(String id, int startX, int startY, int initialBattery) {
        this.id = id;
        this.x = startX;
        this.y = startY;
        this.battery = Math.max(0, Math.min(100, initialBattery));
    }

    public String getId() {
        return id;
    }

    public int getBattery() {
        return battery;
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

    public RobotTask getCurrentTask() {
        return currentTask;
    }

    public void setTask(RobotTask task) {
        currentTask = task;
    }

    public void moveTo(int nx, int ny) {
        status = Status.BUSY;
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
        status = Status.READY;
    }

    public void sleepMinutes(long minutes) {
        sleepMillis(minutes * 1000L);
    }

    public void sleepMillis(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public void increaseBatteryPercent(int pct) {
        battery = Math.max(0, Math.min(100, battery + pct));
    }

    void drainBattery(int pct) {
        battery = Math.max(0, battery - pct);
    }

    public void onChargeWait() {
        status = Status.WAITING;
        // todo: log charging waiting
    }

    public void onChargeStart() {
        status = Status.CHARGING;
        // todo: log charging started
    }

    public void onChargeComplete() {
        status = Status.READY;
        // todo: log charging completed
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        // todo: log Robot thread started
        while (running) {
            RobotTask task = currentTask;
            if (task == null) {
                currentTask = RobotTask.idle();
                continue;
            }

            switch (task.getType()) {
                case MOVE:
                    moveTo(task.getTargetX(), task.getTargetY());
                    currentTask = RobotTask.idle();
                    break;
                case STORE:
                // todo: simulate
                case RETRIEVE:
                    moveTo(task.getTargetX(), task.getTargetY());
                    drainBattery(2);
                    sleepMinutes(1);
                    currentTask = RobotTask.idle();
                    break;
                case CHARGE:
                	//todo: move to charging station
                	status = Status.WAITING;
                case IDLE:
                default:
                    sleepMinutes(1);
                    break;
            }
        }
        // todo: log Robot thread stopped
    }
}