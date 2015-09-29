package server;

import javax.jws.WebService;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by justindomingue on 15-09-27.
 */

//@WebService(endpointInterface = "server.ws.ResourceManager")
public class ResourceManagerImpl implements server.ws.ResourceManager {
    private Map<String, ReservableItem> map;

    public ResourceManagerImpl() {
        map = new HashMap<String, ReservableItem>();
    }

    public synchronized ReservableItem getItem(int id, String key) {
        return map.get(key);
    }

    @Override
    public synchronized boolean addItem(int id, ReservableItem item) {
        String key = item.getKey();
        ReservableItem ret;

        // Item already in db - update
        if (map.containsKey(key)) {
            ReservableItem saved = map.get(key);
            saved.setCount(saved.getCount() + item.getCount());
            if (item.getPrice() > 0) {
                saved.setPrice(item.getPrice());
            }
            ret = map.put(key, saved);

        // Item not in db - create
        } else {
            ret = map.put(key, item);
        }

        return ret != null;
    }

    @Override
    public synchronized boolean removeItem(int id, String key) {
        return map.remove(key) == null;
    }
}
