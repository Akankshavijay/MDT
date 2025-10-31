package main.java.storageManagement;

import java.util.Random;
import java.util.List;
import java.util.Optional;

public class Bin implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private boolean occupied;
    private Item item;
    private final int x;
    private final int y;
    private double distanceToEntry;
    private double distanceToExit;

    public Bin(String id, int capacity) {
        this.id = id;
        Random rand = new Random();
        this.x = rand.nextInt(100);
        this.y = rand.nextInt(100);
    }

    public String getId() { return id; }
    public boolean isOccupied() { return occupied; }
    public Optional<Item> getItem() { return Optional.ofNullable(item); }
    public int getX() { return x; }
    public int getY() { return y; }
    public double getDistanceToEntry() { return distanceToEntry; }
    public double getDistanceToExit() { return distanceToExit; }

    public synchronized void setItem(Item item) {
        if (item == null) {
            this.item = null;
            this.occupied = false;
            return;
        }
        if (occupied) throw new IllegalStateException("Bin already occupied");
        this.item = item;
        this.occupied = true;
    }

    public void computeDistances(List<int[]> entryCoords, List<int[]> exitCoords) {
        double minEntry = Double.MAX_VALUE;
        double minExit = Double.MAX_VALUE;

        for (int[] e : entryCoords) {
            double dist = Math.sqrt(Math.pow(x - e[0], 2) + Math.pow(y - e[1], 2));
            if (dist < minEntry) minEntry = dist;
        }
        for (int[] e : exitCoords) {
            double dist = Math.sqrt(Math.pow(x - e[0], 2) + Math.pow(y - e[1], 2));
            if (dist < minExit) minExit = dist;
        }

        this.distanceToEntry = minEntry;
        this.distanceToExit = minExit;
    }
}
