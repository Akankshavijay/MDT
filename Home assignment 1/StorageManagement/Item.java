package StorageManagement;

import java.io.Serializable;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String id;
    private final String type;

    public Item(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getId() { return id; }
    public String getType() { return type; }

    @Override
    public String toString() {
        return "Item{" + id + ", type=" + type + "}";
    }
}
