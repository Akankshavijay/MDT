package main.java.robotManagement;

import java.util.*;

public class RobotTask {
	public enum Type {
		MOVE, STORE, RETRIEVE, CHARGE, IDLE
	}

	private final String id;
	private final Type type;
	private final int x;
	private final int y;

	private RobotTask(Type type, int x, int y) {
		this.id = UUID.randomUUID().toString();
		this.type = type;
		this.x = x;
		this.y = y;
	}

	public static RobotTask moveTo(int x, int y) {
		return new RobotTask(Type.MOVE, x, y);
	}

	public static RobotTask storeAt(int x, int y) {
		return new RobotTask(Type.STORE, x, y);
	}

	public static RobotTask retrieveFrom(int x, int y) {
		return new RobotTask(Type.RETRIEVE, x, y);
	}

	public static RobotTask chargeAt(int x, int y) {
		return new RobotTask(Type.CHARGE, x, y);
	}

	public static RobotTask idle() {
		return new RobotTask(Type.IDLE, 0, 0);
	}

	public String getId() {
		return id;
	}

	public Type getType() {
		return type;
	}

	public int getTargetX() {
		return x;
	}

	public int getTargetY() {
		return y;
	}

	@Override
	public String toString() {
		return "RobotTask{" + type + ", id=" + id + ", target=(" + x + "," + y + ")}";
	}
}