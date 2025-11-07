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
    public void oneToOne() {
    	
    }
    
    @Test
    public void oneToMany() {
    	
    }
    
    @Test 
    public void manyToOne() {
    	
    }
    
    @Test
    public void manyToMany() {
    	LogManager logger = new LogManager();
    	ChargingManager cm = new ChargingManager("ChargingManager", logger);
    	ChargingStation s1 = new ChargingStation("S1", "ChargingStation", logger);
    	ChargingStation s2 = new ChargingStation("S2", "ChargingStation", logger);
    	cm.addStation(s1);
    	cm.addStation(s2);

    	RobotManager rm = new RobotManager("RobotManager", logger);
    	rm.setChargingManager(cm);
    	Robot r1 = new Robot("R1", 0, 0, 55, "Robot", logger);
    	Robot r2 = new Robot("R2", 5, 5, 18, "Robot", logger);
    	rm.addRobot(r1);
    	rm.addRobot(r2);

    	new Thread(cm, "ChargingManager").start();
    	new Thread(rm, "RobotManager").start();

    	rm.submitTask(RobotTask.move(10, 3));
    }
}