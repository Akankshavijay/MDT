package main.java.storageManagement;

import java.io.Serializable;
import java.util.Optional;

public class Bin implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private boolean occupied;
    private Item item;
    private final int x;
    private final int y;

    public Bin(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.occupied = false;
        this.item = null;
    }

    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }

    public boolean isOccupied() { return occupied; }

    public Optional<Item> getItem() { return Optional.ofNullable(item); }

    public void put(Item item) {
        this.item = item;
        this.occupied = true;
    }

    public Item take() {
        Item out = this.item;
        this.item = null;
        this.occupied = false;
        return out;
    }

    @Override
    public String toString() {
        return "Bin{" + id + ", occupied=" + occupied + ", at=(" + x + "," + y + ")}";
    }
}
