package middleware;

import server.RMItem;

/**
 * Created by justindomingue on 2015-11-06.
 */
public class Operation {
    private String key;
    private RMItem item;
    private String rkey;    //reserveditem key
    private int type;       //0-overwrite, 1-delete, 2-write new //3 - READ // 4- reserve

    private int oldCount;
    private int oldPrice;

    public Operation(String key, RMItem item) {
        this.key = key;
        this.item = item;
        this.type = 0;
    }

    public Operation(String key, RMItem item, int type) {
        this(key, item);
        this.type = type;
    }

    public Operation(String key, RMItem item, String rkey, int oldCount, int oldPrice) {
        this.key = key;
        this.item = item;
        this.rkey = rkey;
        this.oldCount = oldCount;
        this.oldPrice = oldPrice;
        this.type = 4;
    }

    public boolean isOvewrite() { return this.type == 0; }
    public boolean isDelete() { return this.type == 1; }
    public boolean isAdd() { return this.type == 2; }
    public boolean isRead() { return this.type == 3;}
    public boolean isReserve() { return this.type == 4; }

    public String getKey() { return this.key; }
    public RMItem getItem() { return this.item; }
    public String getRKey() { return this.rkey; }
    public int getOldCount() { return this.oldCount; }
    public int getOldPrice() { return this.oldPrice; }
}
