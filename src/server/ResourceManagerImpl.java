package server;

import javax.jws.WebService;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by justindomingue on 15-09-27.
 */

//@WebService(endpointInterface = "server.ws.ResourceManager")
public class ResourceManagerImpl implements server.ws.ResourceManager {
    private Map<Integer, ReservableItem> map;

    public ResourceManagerImpl() {
        map = new HashMap<Integer, ReservableItem>();
    }

    @Override
    public ReservableItem getItem(int key) {
        return map.get(key);
    }

    @Override
    public boolean addItem(int key, ReservableItem item) {
        if (map.containsKey(key)) { return false; }

        map.put(key, item);
        return true;
    }

    @Override
    public boolean removeItem(int key) {
        return map.remove(key) == null;
    }
}
