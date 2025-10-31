package main.java.taskManager;

import java.io.Serializable;
import java.time.LocalDateTime;


public class WarehouseTask implements Serializable {
    private static final long serialVersionUID = 1L;

    // store / retrieve
    private final String action;
    private final String id;
    private final String binId;
    private final String itemId;
    private final String itemType;

    private TaskState state;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public WarehouseTask(String id, String action, String binId, String itemId, String itemType) {
        this.id = id;
        this.action = action;
        this.binId = binId;
        this.itemId = itemId;
        this.itemType = itemType;
        this.state = TaskState.STANDING_BY;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() { return id; }
    public String getAction() { return action; }
    public String getBinId() { return binId; }
    public String getItemId() { return itemId; }
    public String getItemType() { return itemType; }

    public TaskState getState() { return state; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setState(TaskState newState) {
        this.state = newState;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Task[" + id + "] " + action.toUpperCase() + " bin=" + binId +
                " item=" + itemId + " (" + itemType + ") state=" + state;
    }
}
