package middleware;

import server.RMItem;

/**
 * Created by justindomingue on 2015-11-06.
 */
public class Operation {
    private String key;
    private RMItem item;
    private int type;       //0-overwrite, 1-delete, 2-write new //3 - READ

    public Operation(String key, RMItem item) {
        this.key = key;
        this.item = item;
        this.type = 0;
    }

    public Operation(String key, RMItem item, int type) {
        this(key, item);
        this.type = type;
    }

    public boolean isOvewrite() { return this.type == 0; }
    public boolean isDelete() { return this.type == 1; }
    public boolean isAdd() { return this.type == 2; }
    public boolean isRead() { return this.type == 3;}

    public String getKey() { return this.key; }
    public RMItem getItem() { return this.item; }
}
