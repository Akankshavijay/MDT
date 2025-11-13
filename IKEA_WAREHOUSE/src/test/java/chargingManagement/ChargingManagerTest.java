package test.java.chargingManagement;

import org.junit.Test;

import main.java.chargingManagement.ChargingManager;
import main.java.chargingManagement.ChargingStation;
import main.java.logging.LogManager;
import main.java.robotManagement.Robot;

import static org.junit.Assert.*;

public class ChargingManagerTest {
    private static class TestChargingManager extends ChargingManager {
        public TestChargingManager(String systemName, LogManager logger) {
            super(systemName, logger);
        }
        public void loopOncePublic() {
            super.loopOnce();
        }
    }

    @Test
    public void robotFromQueueIsAssignedToReadyStation() {
        LogManager logger = new LogManager();
        TestChargingManager cm = new TestChargingManager("ChargingSysUnit", logger);

        ChargingStation s1 = new ChargingStation("S1", 0, 0, "ChargingSysUnit", logger);
        cm.addStation(s1);

        Robot r1 = new Robot("R1", "RobotSysUnit", logger);
        cm.addRobotToQueue(r1);

        assertEquals(ChargingStation.Status.READY, s1.getStatus());
        assertNull(s1.getCurrentRobot());
        assertTrue(cm.isQueued(r1));

        cm.loopOncePublic();

        assertSame("Robot should be assigned to station", r1, s1.getCurrentRobot());
        assertEquals("Station must be in CHARGING state after assignment",
                ChargingStation.Status.CHARGING, s1.getStatus());
        assertFalse("Robot should be removed from queue after assignment", cm.isQueued(r1));
    }

    @Test
    public void stationWithErrorDoesNotGetAssignedRobot() {
        LogManager logger = new LogManager();
        TestChargingManager cm = new TestChargingManager("ChargingSysUnit2", logger);

        ChargingStation s1 = new ChargingStation("S1", 0, 0, "ChargingSysUnit2", logger);
        cm.addStation(s1);

        Robot r1 = new Robot("R1", "RobotSysUnit2", logger);
        cm.addRobotToQueue(r1);

        s1.setStatus(ChargingStation.Status.ERROR);

        cm.loopOncePublic();

        assertTrue("Robot should remain queued if station is ERROR", cm.isQueued(r1));
        assertNull("Station should not have robot assigned when ERROR", s1.getCurrentRobot());
        assertEquals(ChargingStation.Status.ERROR, s1.getStatus());
    }

    @Test
    public void queuePositionReflectsOrder() {
        LogManager logger = new LogManager();
        TestChargingManager cm = new TestChargingManager("ChargingSysUnit3", logger);

        Robot r1 = new Robot("R1", "RobotSysUnit3", logger);
        Robot r2 = new Robot("R2", "RobotSysUnit3", logger);
        Robot r3 = new Robot("R3", "RobotSysUnit3", logger);

        cm.addRobotToQueue(r1);
        cm.addRobotToQueue(r2);
        cm.addRobotToQueue(r3);

        assertEquals("r1 must be first in queue", 0, cm.queuePosition(r1));
        assertEquals("r2 must be second in queue", 1, cm.queuePosition(r2));
        assertEquals("r3 must be third in queue", 2, cm.queuePosition(r3));
    }
}
