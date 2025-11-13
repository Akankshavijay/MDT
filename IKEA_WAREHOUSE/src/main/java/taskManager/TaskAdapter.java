package main.java.taskManager;

import main.java.robotManagement.RobotTask;
import main.java.storageManagement.Bin;

public final class TaskAdapter {
	private TaskAdapter() {
	}

	public static RobotTask toRobotTask(WarehouseTask wt, Bin targetBin) {
		switch (wt.getType()) {
		case STORE:
			return RobotTask.storeAt(targetBin.getX(), targetBin.getY());
		case RETRIEVE:
			return RobotTask.retrieveFrom(targetBin.getX(), targetBin.getY());
		default:
			throw new IllegalArgumentException("Unsupported warehouse task type: " + wt.getType());
		}
	}
}
