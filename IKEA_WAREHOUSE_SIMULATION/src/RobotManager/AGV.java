package RobotManager;

import Logging.LogManager;
import java.util.Random;

public class AGV {
    private final String id;
    private final String type;
    private String state = "free";
    private int x, y;
    private int batteryLevel = 100;
    private final LogManager logger;
    private final Random rand = new Random();

    public AGV(String id, String type, LogManager logger) {
        this.id = id;
        this.type = type;
        this.logger = logger;
        this.x = rand.nextInt(100);
        this.y = rand.nextInt(100);
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getState() { return state; }
    public boolean isFree() { return state.equals("free"); }
    public boolean isLowBattery() { return batteryLevel <= 20; }
    public int getBatteryLevel() { return batteryLevel; }

    public synchronized void execute(String task, int targetX, int targetY, double workTime) {
        state = "in-progress";
        double distance = Math.sqrt(Math.pow(targetX - x, 2) + Math.pow(targetY - y, 2));
        double totalTime = distance + workTime;

        logAGV("STARTED", task, targetX, targetY, distance);
        try {
            Thread.sleep((long) (totalTime * 1000));
        } catch (InterruptedException ignored) {}

        x = targetX;
        y = targetY;
        simulateDischarge(workTime + distance);
        logAGV("COMPLETED", task, targetX, targetY, distance);
        state = isLowBattery() ? "low-battery" : "free";
    }

    private void simulateDischarge(double workTime) {
        int drain = (int) Math.min(40, workTime / 2);
        batteryLevel = Math.max(0, batteryLevel - drain);
        logger.logAGV(id, String.format("[BATTERY] %s | Battery=%d%% | State=%s", id, batteryLevel, state));
    }

    private void logAGV(String phase, String task, int targetX, int targetY, double distance) {
        String msg = String.format("[%s] %s | %s | Dist=%.2fm | Battery=%d%% | State=%s",
                type.toUpperCase(), id, task, distance, batteryLevel, state);
        logger.logAGV(id, msg);
    }
}
