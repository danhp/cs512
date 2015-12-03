package middleware;

import server.RMItem;

import java.io.Serializable;

public class Operation implements Serializable{
    private String key;
    private int itemType;
    private int type;       //0-overwrite, 1-delete, 2-write 3-read

    public Operation(String key, int item) {
        this.key = key;
        this.itemType = item;
        this.type = 0;
    }

    public Operation(String key, int item, int type) {
        this(key, item);
        this.type = type;
    }

    public boolean isOvewrite() { return this.type == 0; }
    public boolean isDelete() { return this.type == 1; }
    public boolean isAdd() { return this.type == 2; }

    public String getKey() { return this.key; }
    public int getItem() { return this.itemType; }
    public int getType() { return this.type; }
}
