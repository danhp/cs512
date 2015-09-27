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
    public boolean removeItem(int key) {
        return map.remove(key) == null;
    }
}
