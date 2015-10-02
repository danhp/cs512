// -------------------------------
// Adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package server;

import client.TCPMessage;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import javax.jws.WebService;


// A ResourceManager only handles one type of reservable items
public class ResourceManagerImpl {
    // Will only contain ONE RESERVABLEITEM SUBCLASS OBJECTS
    private Map<String, ReservableItem> map;

    private ServerSocket serverSocket;

    public ResourceManagerImpl(int port) {
        map = new HashMap<String, ReservableItem>();

        try {
            serverSocket = new ServerSocket(port);

            System.out.println("Initialized server socket at " + serverSocket.getLocalSocketAddress() + ":" + serverSocket.getLocalPort());

            while (true) {
                // Accepting new connections
                Socket clientSocket = serverSocket.accept();

                // Span new thread to handle connection
                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream());
                            ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());

                            System.out.println("Established connection with " + clientSocket.getRemoteSocketAddress());

                            while (true) {
                                TCPMessage incoming = (TCPMessage) input.readObject();

                                TCPMessage outgoing = handleRequest(incoming);
                                output.writeObject(outgoing);
                            }

                        } catch (IOException e) {
                            System.out.println(e);
                        } catch (ClassNotFoundException e) {
                            System.out.println(e);
                        }


                    }
                }).run();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private synchronized ReservableItem getItem(int id, String key) {
        return map.get(key);
    }

    private synchronized boolean addItem(int id, ReservableItem item) {
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

    private synchronized boolean removeItem(int id, String key) {
        return map.remove(key) == null;
    }

    private TCPMessage handleRequest(TCPMessage incoming) {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put("type", -1); // answer type
        map.put("itemType", incoming.itemType);
        map.put("actionType", incoming.actionType);

        switch (incoming.actionType) {
            case 0:
                ReservableItem item = getItem(incoming.id, incoming.key);
                if (item != null) {
                    map.put("key", item.getKey());
                    map.put("count", item.getCount());
                    map.put("count2", item.getReserved());
                    map.put("success", true);
                } else {
                    map.put("success", false);
                }
                break;
            case 1:
                map.put("success", addItem(incoming.id, new ReservableItem(incoming.key, incoming.count, incoming.price)));
                break;
            case 2:
                map.put("success", removeItem(incoming.id, incoming.key));
                break;
            case 3:
//                map = reserveItem(incoming.id, incoming.customerId, Integer.parseInt(incoming.key));
                break;
        }

        return encode(map);
    }

    public TCPMessage encode(Map<String, Object> dict) {
        TCPMessage msg = new TCPMessage();

        msg.id = (int) (dict.get("id") != null ? dict.get("id") : 0);
        msg.type = (int) (dict.get("type") != null ? dict.get("type") : 0);
        msg.itemType = (int) (dict.get("itemType") != null ? dict.get("itemType") : 0);
        msg.actionType = (int) (dict.get("actionType") != null ? dict.get("actionType") : 0);
        msg.key = (String) (dict.get("key") != null ? dict.get("key") : "");
        msg.count = (int) (dict.get("count") != null ? dict.get("count") : 0);
        msg.count2 = (int) (dict.get("count2") != null ? dict.get("count2") : 0);
        msg.car = (boolean) (dict.get("car") != null ? dict.get("car") : true);
        msg.room = (boolean) (dict.get("room") != null ? dict.get("room") : true);
        msg.customerId = (int) (dict.get("customerId") != null ? dict.get("customerId") : 0);
        msg.success = (boolean) (dict.get("success") != null ? dict.get("success") : false);

        return msg;
    }

    // Reserve an item.
    protected boolean reserveItem(int id, int customerId,
                                  String key, String location) {
//        Trace.info("RM::reserveItem(" + id + ", " + customerId + ", "
//                + key + ", " + location + ") called.");
//        // Read customer object if it exists (and read lock it).
//        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
//        if (cust == null) {
//            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
//                    + key + ", " + location + ") failed: customer doesn't exist.");
//            return false;
//        }
//
//        // Check if the item is available.
//        ReservableItem item = (ReservableItem) readData(id, key);
//        if (item == null) {
//            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
//                    + key + ", " + location + ") failed: item doesn't exist.");
//            return false;
//        } else if (item.getCount() == 0) {
//            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
//                    + key + ", " + location + ") failed: no more items.");
//            return false;
//        } else {
//            // Do reservation.
//            cust.reserve(key, location, item.getPrice());
//            writeData(id, cust.getKey(), cust);
//
//            // Decrease the number of available items in the storage.
//            item.setCount(item.getCount() - 1);
//            item.setReserved(item.getReserved() + 1);
//
//            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
//                    + key + ", " + location + ") OK.");
//            return true;
//        }
        return true;
    }
}
