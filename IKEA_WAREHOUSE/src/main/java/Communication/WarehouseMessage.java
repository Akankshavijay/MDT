package main.java.Communication;

import java.io.Serializable;

public class WarehouseMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String action;   // "store" or "retrieve"
    private final String binId;
    private final String itemId;
    private final String itemType;

    public WarehouseMessage(String action, String binId, String itemId, String itemType) {
        this.action = action;
        this.binId = binId;
        this.itemId = itemId;
        this.itemType = itemType;
    }

    public String getAction() { return action; }
    public String getBinId() { return binId; }
    public String getItemId() { return itemId; }
    public String getItemType() { return itemType; }

    @Override
    public String toString() {
        return action.toUpperCase() + " | Bin=" + binId + " | Item=" + itemId + " (" + itemType + ")";
    }
}
