package main.java.taskManager;

import java.io.Serializable;
import java.time.LocalDateTime;

public class WarehouseTask implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String id;
	private final TaskType type;
	private String binId;
	private final String itemId;
	private final String itemType;

	private TaskState state = TaskState.STANDING_BY;
	private final LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime updatedAt = LocalDateTime.now();

	public WarehouseTask(String id, TaskType type, String binId, String itemId, String itemType) {
		this.id = id;
		this.type = type;
		this.binId = binId;
		this.itemId = itemId;
		this.itemType = itemType;
	}

	public String getId() {
		return id;
	}

	public TaskType getType() {
		return type;
	}

	public String getBinId() {
		return binId;
	}

	public void setBinId(String binId) {
		this.binId = binId;
		touch();
	}

	public String getItemId() {
		return itemId;
	}

	public String getItemType() {
		return itemType;
	}

	public TaskState getState() {
		return state;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setState(TaskState newState) {
		this.state = newState;
		touch();
	}

	private void touch() {
		this.updatedAt = java.time.LocalDateTime.now();
	}

	@Override
	public String toString() {
		return "WarehouseTask[" + id + "] " + type + " bin=" + binId + " item=" + itemId + " (" + itemType + ") state="
				+ state;
	}
}
