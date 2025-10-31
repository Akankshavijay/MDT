package main.java.storageManagement;

import main.java.communication.WarehouseMessage;
import main.java.exceptionHandler.WarehouseException;
import main.java.logging.LogManager;
import main.java.warehouseMap.WarehouseMap;

import java.io.*;
import java.util.*;

public class StorageManager {
    private final Map<String, Bin> bins = new HashMap<>();
    private final LogManager logger;
    private final WarehouseException handler;
    private final String systemName;
    private final int maxBins;
    private final WarehouseMap warehouseMap;
    private ObjectOutputStream outStream;

    public StorageManager(String systemName, LogManager logger, WarehouseException handler, WarehouseMap warehouseMap, int maxBins) {
        this.systemName = systemName;
        this.logger = logger;
        this.handler = handler;
        this.maxBins = maxBins;
        this.warehouseMap = warehouseMap;
        logger.log(systemName, "Storage system initialized with max bins: " + maxBins);
    }

    public void connectStream(ObjectOutputStream stream) {
        this.outStream = stream;
    }

    public void addBin(Bin bin) {
        handler.handleWarehouseOperation(systemName, () -> {
            if (bins.size() >= maxBins)
                throw new RuntimeException("Cannot add more bins, limit reached.");

            List<int[]> entries = new ArrayList<>(warehouseMap.getEntryPoints().values());
            List<int[]> exits = new ArrayList<>(warehouseMap.getExitPoints().values());
            bin.computeDistances(entries, exits);
            bins.put(bin.getId(), bin);
        });
    }

    public void requestStore(String binId, Item item) {
        try {
            WarehouseMessage msg = new WarehouseMessage("store", binId, item.getId(), item.getType());
            outStream.writeObject(msg);
            outStream.flush();
            logger.log(systemName, "Stream → Sent STORE request: " + msg);
        } catch (IOException e) {
            handler.handleWarehouseOperation(systemName, () -> { throw new RuntimeException(e); });
        }
    }

    public void requestRetrieve(String binId, Item item) {
        try {
            WarehouseMessage msg = new WarehouseMessage("retrieve", binId, item.getId(), item.getType());
            outStream.writeObject(msg);
            outStream.flush();
            logger.log(systemName, "Stream → Sent RETRIEVE request: " + msg);
        } catch (IOException e) {
            handler.handleWarehouseOperation(systemName, () -> { throw new RuntimeException(e); });
        }
    }

    public boolean isBinOccupied(String binId) {
        Bin bin = bins.get(binId);
        return bin != null && bin.isOccupied();
    }

    public Optional<Item> getItem(String binId) {
        Bin bin = bins.get(binId);
        return bin != null ? bin.getItem() : Optional.empty();
    }
}
