package test.java.chargingManagement;

import org.junit.Test;

import main.java.chargingManagement.ChargingManager;
import main.java.chargingManagement.ChargingStation;
import main.java.logging.LogManager;
import main.java.robotManagement.RobotManager;
import main.java.robotManagement.RobotTask;
import main.java.robotManagement.Robot;

import static org.junit.Assert.*;

public class ChargingManagerTest {
    
    
    @Test
    public void possitiveTest() throws InterruptedException {
    	LogManager logger = new LogManager();
    	ChargingManager cm = new ChargingManager("ChargingManager", logger);
    	ChargingStation s1 = new ChargingStation("S1", "ChargingStation", logger);
    	ChargingStation s2 = new ChargingStation("S2", "ChargingStation", logger);
    	cm.addStation(s1);
    	cm.addStation(s2);

    	RobotManager rm = new RobotManager("RobotManager", logger);
    	rm.setChargingManager(cm);
    	Robot r1 = new Robot("R1", 0, 0, 55, "Robot", logger);
    	Robot r2 = new Robot("R2", 5, 5, 21, "Robot", logger);
    	rm.addRobot(r1);
    	rm.addRobot(r2);

    	cm.start();
    	rm.start();

    	Thread.sleep(1000);
    	r2.drainBattery(3);
    	Thread.sleep(9000);
    	
    	s1.stop(); s2.stop();
    	r1.stop();r2.stop();
    	cm.stop(); rm.stop();
    }
    
    @Test 
    public void negativeTest() throws InterruptedException {
    	LogManager logger = new LogManager();
    	ChargingManager cm = new ChargingManager("ChargingManager", logger);
    	ChargingStation s1 = new ChargingStation("S1", "ChargingStation", logger);
    	cm.addStation(s1);

    	RobotManager rm = new RobotManager("RobotManager", logger);
    	rm.setChargingManager(cm);
    	Robot r1 = new Robot("R1", 0, 0, 15, "Robot", logger);
    	Robot r2 = new Robot("R2", 0, 1, 15, "Robot", logger);
    	Robot r3 = new Robot("R3", 0, 2, 15, "Robot", logger);
    	Robot r4 = new Robot("R4", 0, 3, 15, "Robot", logger);
    	Robot r5 = new Robot("R5", 0, 4, 15, "Robot", logger);
    	rm.addRobot(r1);
    	rm.addRobot(r2);
    	rm.addRobot(r3);
    	rm.addRobot(r4);
    	rm.addRobot(r5);

    	cm.start();
    	rm.start();

    	Thread.sleep(10000);
    	
    	s1.stop();
    	r1.stop();r2.stop();r3.stop();r4.stop();r5.stop();
    	cm.stop(); rm.stop();
    }
}