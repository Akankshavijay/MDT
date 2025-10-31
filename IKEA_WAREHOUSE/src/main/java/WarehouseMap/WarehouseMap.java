package main.java.WarehouseMap;

import java.util.*;

import main.java.StorageManagement.Bin;

public class WarehouseMap implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, Bin> binMap = new HashMap<>();
    private final Map<String, int[]> entryPoints = new HashMap<>();
    private final Map<String, int[]> exitPoints = new HashMap<>();
    private final Map<String, int[]> chargingStations = new HashMap<>();

    public WarehouseMap(int numEntries, int numExits, int numChargingSlots) {
        initializeGates(numEntries, numExits);
        initializeChargingStations(numChargingSlots);
    }

    private void initializeGates(int numEntries, int numExits) {
        Random rand = new Random();
        for (int i = 1; i <= numEntries; i++) {
            String name = "EntryGate-" + (char) ('A' + i - 1);
            entryPoints.put(name, new int[]{rand.nextInt(100), rand.nextInt(100)});
        }
        for (int i = 1; i <= numExits; i++) {
            String name = "ExitGate-" + i;
            exitPoints.put(name, new int[]{rand.nextInt(100), rand.nextInt(100)});
        }
    }

    private void initializeChargingStations(int numSlots) {
        Random rand = new Random();
        for (int i = 1; i <= numSlots; i++) {
            String name = "ChargeSlot-" + i;
            chargingStations.put(name, new int[]{rand.nextInt(100), rand.nextInt(100)});
        }
    }

    public void addBin(Bin bin) {
        binMap.put(bin.getId(), bin);
    }

    public Map<String, Bin> getBins() { return binMap; }
    public Map<String, int[]> getEntryPoints() { return entryPoints; }
    public Map<String, int[]> getExitPoints() { return exitPoints; }
    public Map<String, int[]> getChargingStations() { return chargingStations; }

    public Set<String> listChargingSlots() { return chargingStations.keySet(); }

    public void printMap() {
        System.out.println("Warehouse Spatial Map");
        System.out.println("Entry Gates:");
        for (var e : entryPoints.entrySet())
            System.out.printf("  %s -> (%d,%d)%n", e.getKey(), e.getValue()[0], e.getValue()[1]);
        System.out.println("Exit Gates:");
        for (var e : exitPoints.entrySet())
            System.out.printf("  %s -> (%d,%d)%n", e.getKey(), e.getValue()[0], e.getValue()[1]);
        System.out.println("Charging Stations:");
        for (var c : chargingStations.entrySet())
            System.out.printf("  %s -> (%d,%d)%n", c.getKey(), c.getValue()[0], c.getValue()[1]);
        System.out.println("Bins:");
        for (var b : binMap.values())
            System.out.printf("  %s -> (%d,%d)%n", b.getId(), b.getX(), b.getY());
    }
}
