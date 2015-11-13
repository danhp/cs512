package server;

/**
 * Created by justindomingue on 2015-11-06.
 */
public class Operation {
    private String key;
    private RMItem item;
    private int type;       //0-overwrite, 1-delete, 2-write new, 3 - READ, 4 - reserve

    private int oldcount;
    private int oldreserved;

    public Operation(String key, RMItem item) {
        this.key = key;
        this.item = item;
        this.type = 0;
    }

    public Operation(String key, RMItem item, int type) {
        this(key, item);
        this.type = type;
    }

    public Operation(String key, RMItem item, int oldcount, int oldreserved) {
        this (key, item);
        this.type = 4;
        this.oldcount = oldcount;
        this.oldreserved = oldreserved;
    }

    public boolean isOvewrite() { return this.type == 0; }
    public boolean isDelete() { return this.type == 1; }
    public boolean isAdd() { return this.type == 2; }
    public boolean isRead() { return this.type == 3; }
    public boolean isReserve() { return this.type == 4; }

    public String getKey() { return this.key; }
    public RMItem getItem() { return this.item; }
    public int getOldCount() { return this.oldcount; }
    public int getOldReserved() { return this.oldreserved; }

    @Override
    public String toString() {
        return "Operation("+key+","+type+")";
    }
}
