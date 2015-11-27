package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;
import server.*;
import server.ws.InvalidTransactionException;
import server.ws.TransactionAbortedException;

import javax.jws.WebService;
import java.net.MalformedURLException;
import java.util.*;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

@WebService(endpointInterface = "middleware.ws.MiddleWare")
public class MiddleWareImpl implements middleware.ws.MiddleWare {

    middleware.ResourceManagerImplService rm;
    middleware.ResourceManager[] proxy;

    private static int CAR_PROXY_INDEX = 0;
    private static int FLIGHT_PROXY_INDEX = 1;
    private static int ROOM_PROXY_INDEX = 2;

    private TransactionManager tm = new TransactionManager(this);

    public MiddleWareImpl() {
        String hosts[] = {"142.157.169.58","142.157.169.58","142.157.165.27" };
//        String hosts[] = {"localhost","localhost","localhost" };
        int[] ports = {4000,4001,4002};

        setupProxies(hosts, ports);
    }

    public MiddleWareImpl(String[] hosts, int[] ports) {
        if (ports.length != hosts.length) {
            System.out.println("Ports array length doesn't match hosts array length");
            return;
        }

        setupProxies(hosts, ports);
    }

    public void setupProxies(String[] hosts, int[] ports)
    {
        proxy = new middleware.ResourceManager[ports.length];

        try {
            for (int i = 0; i < ports.length; i++) {
                URL wsdlLocation = new URL("http://" + hosts[i] + ":" + ports[i] + "/rm/service?wsdl");
                rm = new middleware.ResourceManagerImplService(wsdlLocation);
                proxy[i] = rm.getResourceManagerImplPort();

                System.out.println("Connection established with " + hosts[i] + ":" + ports[i]);
            }
        } catch (MalformedURLException e) {
            System.out.println(e);
        }
    }


    protected middleware.ResourceManager getCarProxy() { return proxy[MiddleWareImpl.CAR_PROXY_INDEX]; }
    protected middleware.ResourceManager getFlightProxy() { return proxy[MiddleWareImpl.FLIGHT_PROXY_INDEX]; }
    protected middleware.ResourceManager getRoomProxy() { return proxy[MiddleWareImpl.ROOM_PROXY_INDEX]; }

    public middleware.ResourceManager getProxy(String which) {
        if (which.equalsIgnoreCase("flight")) {
            return getFlightProxy().selfDestruct();
        } else if (which.equalsIgnoreCase("car")) {
            return getCarProxy().selfDestruct();
        } else if (which.equalsIgnoreCase("room")) {
            return getRoomProxy().selfDestruct();
        } else {
            return null;
        }
    }

    public void crash(String which) {
        if (which.equalsIgnoreCase("tm")) {
            System.out.println("TM> Initiating self destruct");
            System.exit(-1);
        }

        getProxy(which).selfDestruct();

    }

    public void prepare(int id, String rm)  throws TransactionAbortedException, InvalidTransactionException {
        getProxy(rm).prepare(id);
    }

    // CUSTOMER

    protected RMHashtable customerHT = new RMHashtable();

    private Map<Integer, Map<String, RMItem>> readSet = new ConcurrentHashMap<>();
    private Map<Integer, Map<String, RMItem>> writeSet = new ConcurrentHashMap<>();

    // Read a data item.
    private RMItem readData(int transactionID, String key) {
        synchronized(customerHT) {
            synchronized (readSet) {
                // Check the writeSet
                if (writeSet.get(transactionID).containsKey(key))
                    return writeSet.get(transactionID).get(key);

                // Check the readSet
                if (readSet.get(transactionID).containsKey(key))
                    return readSet.get(transactionID).get(key);

                // Else get data from database
                RMItem item = (RMItem) customerHT.get(key);
                this.readSet.get(transactionID).put(key, item);
                return item;
            }
        }
    }

    // Write a data item.
    private void writeData(int transactionID, String key, RMItem newValue, boolean commit) {
        synchronized(customerHT) {
            if (commit) {
                customerHT.put(key, newValue);
            } else {
                // save the data in the write set.
                this.writeSet.get(transactionID).put(key, newValue);
            }
        }
    }

    // Remove the item out of storage.
    protected void removeData(int transactionID, String key, boolean commit) {
        synchronized(customerHT) {
            if (commit) {
                customerHT.remove(key);
            } else {
                // remove from the write set.
                // tag it with null to mark for deletion
                this.writeSet.get(transactionID).put(key, null);
            }
        }
    }

    // Unreserve an item at the customer as the transaction failed
    private void unreserveItem(int transactionID, int custId, String key) {
        Customer cust = (Customer) readData(transactionID, Customer.getKey(custId));
        if (cust != null) {
            cust.unreserve(key);
        }
    }

    // CUSTOMER TRANSACTIONS HELPERS
    public void startCustomer(int transactionID) {
        synchronized (this.writeSet) {
            if (writeSet.containsKey(transactionID)) return;
            writeSet.put(transactionID, new HashMap<String, RMItem>());
        }
        synchronized (this.readSet) {
            readSet.put(transactionID, new HashMap<String, RMItem>());
        }
    }

    public void commitCustomer(int transactionID) {
        synchronized (writeSet) {
            if (!writeSet.containsKey(transactionID)) return;

            for (Map.Entry<String, RMItem> entry : writeSet.get(transactionID).entrySet()) {
                if (entry.getValue() == null) {
                    this.removeData(transactionID, entry.getKey(), true);
                } else {
                    this.writeData(transactionID, entry.getKey(), entry.getValue(), true);
                }
            }

            this.writeSet.remove(transactionID);
        }
        synchronized (readSet) {
            this.readSet.remove(transactionID);
        }
    }

    public void abortCustomer(int transactionID) {
        synchronized (this.writeSet) {
            if (!writeSet.containsKey(transactionID)) return;
            this.writeSet.remove(transactionID);
        }
        synchronized (this.readSet) {
            this.readSet.remove(transactionID);
        }
    }

    // TRANSACTIONS
    @Override
    public int start() {
        return this.tm.start();
    }

    @Override
    public boolean commit(int id) {
        return this.tm.commit(id);
    }

    @Override
    public boolean abort(int id) {
        return this.tm.abort(id);
    }

    @Override
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 2))) return false;

        return getFlightProxy().addFlight(id, flightNumber, numSeats, flightPrice);
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 2))) return false;

        return getFlightProxy().deleteFlight(id, flightNumber);
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 1))) return 0;

        return getFlightProxy().queryFlight(id, flightNumber);
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 1))) return 0;

        return getFlightProxy().queryFlightPrice(id, flightNumber);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(location, 1, 2))) return false;

        return getCarProxy().addCars(id, location, numCars, carPrice);
    }

    @Override
    public boolean deleteCars(int id, String location) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(location, 1, 2))) return false;

        return getCarProxy().deleteCars(id, location);
    }

    @Override
    public int queryCars(int id, String location) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(location, 1, 1))) return 0;

        return getCarProxy().queryCars(id, location);
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(location, 1, 1))) return 0;

        return getCarProxy().queryCarsPrice(id, location);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(location, 2, 2))) return false;

        return getRoomProxy().addRooms(id, location, numRooms, roomPrice);
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(location, 2, 2))) return false;

        return getRoomProxy().deleteRooms(id, location);
    }

    @Override
    public int queryRooms(int id, String location) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(location, 2, 1))) return 0;

        return getRoomProxy().queryRooms(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(location, 2, 1))) return 0;

        return getRoomProxy().queryRoomsPrice(id, location);
    }

    // Customer operations //

    @Override
    public int newCustomer(int id) {
        Trace.info("INFO: RM::newCustomer(" + id + ") called.");
        // Generate a globally unique Id for the new customer.
        int customerId = Integer.parseInt(String.valueOf(id) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer cust = new Customer(customerId);
        writeData(id, cust.getKey(), cust, false);
        Trace.info("RM::newCustomer(" + id + ") OK: " + customerId);

        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return 0;

        return customerId;
    }

    // This method makes testing easier.
    @Override
    public boolean newCustomerId(int id, int customerId) {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerId + ") called.");

        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            cust = new Customer(customerId);
            writeData(id, cust.getKey(), cust, false);
            Trace.info("INFO: RM::newCustomer(" + id + ", " + customerId + ") OK.");
            return true;
        } else {
            Trace.info("INFO: RM::newCustomer(" + id + ", " +
                    customerId + ") failed: customer already exists.");
            return false;
        }
    }

    // Delete customer from the database.
    @Override
    public boolean deleteCustomer(int id, int customerId) {
        Trace.info("RM::deleteCustomer(" + id + ", " + customerId + ") called.");

        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::deleteCustomer(" + id + ", "
                    + customerId + ") failed: customer doesn't exist.");
            return false;
        } else {

            // Remove the customer from the storage.
            removeData(id, cust.getKey(), false);
            Trace.info("RM::deleteCustomer(" + id + ", " + customerId + ") OK.");
            return true;
        }
    }

    private boolean customerExists(int id, int customerId) {
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        return (cust != null);
    }

    public void setCustomerReservation(int id, int customerId, String key, String location, int price) {
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        cust.reserve(key, location, price);
    }

    // Return data structure containing customer reservation info.
    // Returns null if the customer doesn't exist.
    // Returns empty RMHashtable if customer exists but has no reservations.
    public Object[] getCustomerReservations(int id, int customerId) {
        Trace.info("RM::getCustomerReservations(" + id + ", "
                + customerId + ") called.");

        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 1))) return null;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.info("RM::getCustomerReservations(" + id + ", "
                    + customerId + ") failed: customer doesn't exist.");
            return null;
        } else {
            Collection<Object> col = new ArrayList<Object>();
            col.addAll(cust.getReservations().keySet());

            Collection<ReservedItem> ris = cust.getReservations().values();
            for (ReservedItem ri : ris) {
                col.add(ri.getCount());
            }

            return col.toArray();
        }
    }

    // Return a bill.
    @Override
    public String queryCustomerInfo(int id, int customerId) {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + ") called.");

        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 1))) return null;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", "
                    + customerId + ") failed: customer doesn't exist.");
            // Returning an empty bill means that the customer doesn't exist.
            return "";
        } else {
            String s = cust.printBill();
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + "): \n");
            System.out.println(s);
            return s;
        }
    }

    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        if (!customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Flight.getKey(flightNumber) + ", " + flightNumber + ") failed: customer doesn't exist.");
            return false;
        }

        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        boolean reserved = getFlightProxy().reserveFlight(id, customerId, flightNumber);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Flight.getKey(flightNumber) + ", " + flightNumber + ") failed: flight cannot be reserved.");
            if (!this.tm.addOperation(id, new Operation(Integer.toString(flightNumber), 0, 2))) return false;
            return false;
        }

        int price = queryFlightPrice(id, flightNumber);
        setCustomerReservation(id, customerId, Flight.getKey(flightNumber), String.valueOf(flightNumber), price);

        return true;
    }

    @Override
    public boolean reserveCar(int id, int customerId, String location) {
        if (!customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Car.getKey(location) + ", " + location + ") failed: customer doesn't exist.");
            return false;
        }

        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        boolean reserved = getCarProxy().reserveCar(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Car.getKey(location) + ", " + location + ") failed: flight cannot be reserved.");
            if (!this.tm.addOperation(id, new Operation(location, 1, 2))) return false;
            return false;
        }

        int price = queryCarsPrice(id, location);
        setCustomerReservation(id, customerId, Car.getKey(location), location, price);

        return true;
    }

    @Override
    public boolean reserveRoom(int id, int customerId, String location) {
        if (!customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Room.getKey(location) + ", " + location + ") failed: customer doesn't exist.");
            return false;
        }

        // Save to request locks later
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        boolean reserved = getRoomProxy().reserveRoom(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Room.getKey(location) + ", " + location + ") failed: flight cannot be reserved.");
            if (!this.tm.addOperation(id, new Operation(location, 2, 2))) return false;
            return false;
        }

        int price = queryRoomsPrice(id, location);
        setCustomerReservation(id, customerId, Room.getKey(location), location, price);

        return true;
    }

    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
        if (!this.tm.addOperation(id, new Operation(Integer.toString(customerId), 3, 2))) return false;

        // List to abort in case one command fails.
        Map<Integer, List<String>> saved = new HashMap<>(3);
        saved.put(0, new ArrayList<String>());
        saved.put(1, new ArrayList<String>());
        saved.put(2, new ArrayList<String>());

        // Assuming everything has to work for reserve itinerary to return true
        boolean result = false;

        for (Enumeration<String> e = flightNumbers.elements(); e.hasMoreElements();) {
            int number = Integer.parseInt(e.nextElement());
            result = reserveFlight(id, customerId, number);
            if (result) {
                // Add to list so that if we abort we know which to reset
                saved.get(0).add("flight-" + Integer.toString(number));

                // Save to request locks later
                if (!this.tm.addOperation(id, new Operation(Integer.toString(number), 0, 2))) return false;
            } else {
                this.resetReservation(id, customerId, saved);
                return false;
            }
        }

        if (car) {
            result = reserveCar(id, customerId, location);
            if (result) {
                // Add to list so that if we abort we know which to reset
                saved.get(1).add("car-" + location);

                // Save to request locks later
                if (!this.tm.addOperation(id, new Operation(location, 1, 2))) return false;
            } else {
                this.resetReservation(id, customerId, saved);
                return false;
            }
        }

        if (room) {
            result = reserveRoom(id, customerId, location);
            if (result) {
                // Save to request locks later
                if (!this.tm.addOperation(id, new Operation(location, 2, 2))) return false;
            } else {
                // Failed, so reset everything.
                this.resetReservation(id, customerId, saved);
                return false;
            }
        }

        return result;
    }

    private void resetReservation(int id, int customerId, Map<Integer, List<String>>  map) {
        List<String> flightList = map.get(0);
        for (String s : flightList) {
            this.getFlightProxy().unreserveItem(id, s, s, 1);
            this.unreserveItem(id, customerId, s);
        }

        List<String> carList = map.get(1);
        if (!carList.isEmpty()) {
            this.getCarProxy().unreserveItem(id, carList.get(0), carList.get(0), 1);
            this.unreserveItem(id, customerId, carList.get(0));
        }

        List<String> roomList = map.get(2);
        if (!roomList.isEmpty()) {
            this.getCarProxy().unreserveItem(id, roomList.get(0), roomList.get(0), 1);
            this.unreserveItem(id, customerId, roomList.get(0));
        }
    }

    @Override
    public boolean shutdown() {
        // Check that no transaction are running.
        if (tm.isActive()) return false;

        // Shutdown all the RMs
        try {
            this.getFlightProxy().shutdown();
        } catch (Exception e) {
            // Do nothing as normal
        }
        try {
            this.getCarProxy().shutdown();
        } catch (Exception e) {
            // Do nothing as normal
        }
        try {
            this.getRoomProxy().shutdown();
        } catch (Exception e) {
            // Do nothing as normal
        }

        // Shutdown the middleware
        System.exit(0);

        return true;
    }
}