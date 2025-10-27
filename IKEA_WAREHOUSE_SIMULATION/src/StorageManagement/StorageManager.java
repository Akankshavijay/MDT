package StorageManagement;

import java.util.*;
import warehouse_map.*;
import Logging.LogManager;
import Exception_Handler.WarehouseException;

public class StorageManager {
    private final Map<String, Bin> bins = new HashMap<>();
    private final LogManager logger;
    private final WarehouseException handler;
    private final String systemName;
    private final int maxBins;
    private final WarehouseMap warehouseMap;

    public StorageManager(String systemName, LogManager logger, WarehouseException handler, WarehouseMap warehouseMap, int maxBins) {
        this.systemName = systemName;
        this.logger = logger;
        this.handler = handler;
        this.maxBins = maxBins;
        this.warehouseMap = warehouseMap;
        logger.log(systemName, "Storage system initialized with max bins: " + maxBins);
    }

    public void addBin(Bin bin) {
        handler.handleWarehouseOperation(systemName, () -> {
            if (bins.size() >= maxBins) {
                String msg = "Cannot add bin '" + bin.getId() + "' — warehouse at full capacity (" + maxBins + ")";
                logger.log(systemName, msg);
                throw new RuntimeException(msg);
            }
            if (bins.containsKey(bin.getId())) {
                String msg = "Duplicate bin ID: " + bin.getId();
                logger.log(systemName, msg);
                throw new RuntimeException(msg);
            }

            List<int[]> entries = new ArrayList<>(warehouseMap.getEntryPoints().values());
            List<int[]> exits = new ArrayList<>(warehouseMap.getExitPoints().values());
            bin.computeDistances(entries, exits);

            bins.put(bin.getId(), bin);
            warehouseMap.addBin(bin);

            logger.log(systemName, "Added bin: " + bin.getId() + " (Total bins: " + bins.size() + ")");
            logger.log(systemName, "Bin " + bin.getId() + " location: (" + bin.getX() + "," + bin.getY() + 
                    "), Distance to Entry=" + String.format("%.2f", bin.getDistanceToEntry()) + 
                    "m, Distance to Exit=" + String.format("%.2f", bin.getDistanceToExit()) + "m");
        });
    }

    public void storeItem(String binId, Item item) {
        handler.handleWarehouseOperation(systemName, () -> {
            Bin bin = bins.get(binId);
            if (bin == null) throw new RuntimeException("Bin not found: " + binId);
            bin.setItem(item);
            logger.log(systemName, "Stored " + item + " in bin " + binId);
        });
    }

    public void removeItem(String binId) {
        handler.handleWarehouseOperation(systemName, () -> {
            Bin bin = bins.get(binId);
            if (bin == null) throw new RuntimeException("Bin not found: " + binId);
            bin.setItem(null);
            logger.log(systemName, "Removed item from bin " + binId);
        });
    }

    public boolean isBinOccupied(String binId) {
        Bin bin = bins.get(binId);
        return bin != null && bin.isOccupied();
    }

    public Optional<Item> getItem(String binId) {
        Bin bin = bins.get(binId);
        return bin != null ? bin.getItem() : Optional.empty();
    }

    public int getTotalBins() { return bins.size(); }
    public int getMaxBins() { return maxBins; }
    public WarehouseMap getWarehouseMap() { return warehouseMap; }
}
