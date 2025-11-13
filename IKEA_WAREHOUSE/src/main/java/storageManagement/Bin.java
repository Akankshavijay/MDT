package main.java.storageManagement;

import java.io.Serializable;
import java.util.Optional;

public class Bin implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum Status { FREE, RESERVED, OCCUPIED }
    
    private final String id;
    private final int x;
    private final int y;
    private Item item = null;
    
    private Status status = Status.FREE;
    private String reservedByTaskId = null;

    public Bin(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getId() { return id; }
    public synchronized int getX() { return x; }
    public synchronized int getY() { return y; }
    public synchronized Status getStatus() { return status; }
    public synchronized boolean isOccupied() { return status == Status.OCCUPIED; }
    public synchronized Optional<Item> getItem() { return Optional.ofNullable(item); }

    public synchronized boolean tryReserve(String taskId) {
        if (status != Status.FREE) return false;
        status = Status.RESERVED;
        reservedByTaskId = taskId;
        return true;
    }

    public synchronized void releaseReservation(String taskId) {
        if (status == Status.RESERVED && taskId.equals(reservedByTaskId)) {
            reservedByTaskId = null;
            status = Status.FREE;
        }
    }

    public synchronized void commitStore(String taskId, Item newItem) {
        if (status != Status.RESERVED || !taskId.equals(reservedByTaskId)) {
            throw new IllegalStateException("Bin " + id + " not reserved by task " + taskId);
        }
        if (newItem == null) throw new IllegalArgumentException("Item must not be null");
        item = newItem;
        reservedByTaskId = null;
        status = Status.OCCUPIED;
    }

    public synchronized Item commitRetrieve() {
        if (status != Status.OCCUPIED) {
            throw new IllegalStateException("Bin " + id + " is not occupied");
        }
        Item out = item;
        item = null;
        status = Status.FREE;
        return out;
    }

    @Override
    public String toString() {
        return "Bin{" + id + ", occupied=" + status + ", at=(" + x + "," + y + ")}";
    }
}
