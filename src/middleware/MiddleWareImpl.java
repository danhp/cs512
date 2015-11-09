package middleware;

import LockManager.LockManager;
import middleware.ws.MiddleWare;
import server.*;

import javax.jws.WebService;
import java.net.MalformedURLException;
import java.util.*;
import java.net.URL;

@WebService(endpointInterface = "middleware.ws.MiddleWare")
public class MiddleWareImpl implements middleware.ws.MiddleWare {

    middleware.ResourceManagerImplService rm;
    middleware.ResourceManager[] proxy;

    private static int CAR_PROXY_INDEX = 0;
    private static int FLIGHT_PROXY_INDEX = 1;
    private static int ROOM_PROXY_INDEX = 2;

    private static int OVERWRITE = 0;
    private static int DELETE = 1;
    private static int ADD_NEW = 2;

    private TransactionManagerImpl transactionManager;

    public MiddleWareImpl() {
//        String hosts[] = {"142.157.165.20","142.157.165.20","142.157.165.113","142.157.165.113" };
        String hosts[] = {"localhost","localhost","localhost" };
        int[] ports = {4000,4001,4002};

        setupProxies(hosts, ports);
    }

    public MiddleWareImpl(String[] hosts, int[] ports) {
        if (ports.length != hosts.length) {
            System.out.println("Ports array length doesn't match hosts array length");
            return;
        }

        setupProxies(hosts, ports);
        transactionManager = new TransactionManagerImpl(this, getCarProxy(), getFlightProxy(), getRoomProxy());
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


    private middleware.ResourceManager getCarProxy() { return proxy[MiddleWareImpl.CAR_PROXY_INDEX]; }
    private middleware.ResourceManager getFlightProxy() { return proxy[MiddleWareImpl.FLIGHT_PROXY_INDEX]; }
    private middleware.ResourceManager getRoomProxy() { return proxy[MiddleWareImpl.ROOM_PROXY_INDEX]; }

    // CUSTOMER

    protected RMHashtable m_itemHT = new RMHashtable();

    // Read a data item.
    private RMItem readData(int id, String key) {
        synchronized(m_itemHT) {
            return (server.RMItem) m_itemHT.get(key);
        }
    }

    // Write a data item.
    //NOTE an invalid id means that the operation will not be saved - could be used when undoing an operation
    private void writeData(int id, String key, RMItem oldValue, RMItem newValue) {
        synchronized(m_itemHT) {
            //add operation
            int type = OVERWRITE; // operation is overwrite
            if (oldValue == null)
                type = ADD_NEW; // operation is add
            Transaction t = this.transactionManager.getTransaction(id);
            if (t!=null)
                t.addOperation(new Operation(key, oldValue, type));

            m_itemHT.put(key, newValue);
        }
    }

    // Remove the item out of storage.
    // Invalid transaction id --> operation not saved.
    protected server.RMItem removeData(int id, String key, server.RMItem oldValue) {
        synchronized(m_itemHT) {
            Transaction t = this.transactionManager.getTransaction(id);
            if (t != null)
                this.transactionManager.getTransaction(id).addOperation(new Operation(key, oldValue, DELETE));

            return (server.RMItem) m_itemHT.remove(key);
        }
    }

    @Override
    public void start(int id) {
        transactionManager.start(id);
    }

    @Override
    public void commit(int id) {
        transactionManager.commit(id);
    }

    @Override
    public void abort(int id) {
        transactionManager.abort(id);
    }

    // Undo `operation`
    public void undo(int id, Operation operation) {
        // note id=-1 so that the operation won't be saved
        if (operation.isAdd())
            removeData(-1, operation.getKey(), null);
        else if (operation.isOvewrite() || operation.isDelete())
            writeData(-1, operation.getKey(), null, operation.getItem());
    }


    @Override
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        transactionManager.enlist(id, FLIGHT_PROXY_INDEX);
        return getFlightProxy().addFlight(id, flightNumber, numSeats, flightPrice);
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        transactionManager.enlist(id, FLIGHT_PROXY_INDEX);
        return getFlightProxy().deleteFlight(id, flightNumber);
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        return getFlightProxy().queryFlight(id, flightNumber);
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        return getFlightProxy().queryFlightPrice(id, flightNumber);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        transactionManager.enlist(id, CAR_PROXY_INDEX);
        return getCarProxy().addCars(id, location, numCars, carPrice);
    }

    @Override
    public boolean deleteCars(int id, String location) {
        transactionManager.enlist(id, CAR_PROXY_INDEX);
        return getCarProxy().deleteCars(id, location);
    }

    @Override
    public int queryCars(int id, String location) {
        return getCarProxy().queryCars(id, location);
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        return getCarProxy().queryCarsPrice(id, location);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        transactionManager.enlist(id, ROOM_PROXY_INDEX);
        return getRoomProxy().addRooms(id, location, numRooms, roomPrice);
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        transactionManager.enlist(id, ROOM_PROXY_INDEX);
        return getRoomProxy().deleteRooms(id, location);
    }

    @Override
    public int queryRooms(int id, String location) {
        return getRoomProxy().queryRooms(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
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
        writeData(id, cust.getKey(), null, cust);
        Trace.info("RM::newCustomer(" + id + ") OK: " + customerId);
        return customerId;
    }

    // This method makes testing easier.
    @Override
    public boolean newCustomerId(int id, int customerId) {
        Trace.info("INFO: RM::newCustomer(" + id + ", " + customerId + ") called.");
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            cust = new Customer(customerId);
            writeData(id, cust.getKey(), null, cust);
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
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::deleteCustomer(" + id + ", "
                    + customerId + ") failed: customer doesn't exist.");
            return false;
        } else {

            // Remove the customer from the storage.
            removeData(id, cust.getKey(), cust);
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

        boolean reserved = getFlightProxy().reserveFlight(id, customerId, flightNumber);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Flight.getKey(flightNumber) + ", " + flightNumber + ") failed: flight cannot be reserved.");
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

        boolean reserved = getCarProxy().reserveCar(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Car.getKey(location) + ", " + location + ") failed: flight cannot be reserved.");
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

        boolean reserved = getRoomProxy().reserveRoom(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Room.getKey(location) + ", " + location + ") failed: flight cannot be reserved.");
            return false;
        }

        int price = queryRoomsPrice(id, location);
        setCustomerReservation(id, customerId, Room.getKey(location), location, price);

        return true;
    }

    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
        // Assuming everything has to work for reserve itinerary to return true
        boolean result = false;

        for (Enumeration<String> e = flightNumbers.elements(); e.hasMoreElements();)
        {
            result = reserveFlight(id, customerId, Integer.parseInt(e.nextElement()));
        }

        if (car) {
            result = reserveCar(id, customerId, location);
        }

        if (room) {
            result = reserveRoom(id, customerId, location);
        }

        return result;
    }

}

public class TransactionManagerImpl {

    private Map<Integer, List<Integer>> activeTransactions = new HashMap<Integer, List<Integer>>();
    private List<Transaction> transactions;

    private MiddleWareImpl middleware;
    private middleware.ResourceManager carProxy;
    private middleware.ResourceManager flightProxy;
    private middleware.ResourceManager roomProxy;

    private static int CAR_PROXY_INDEX = 0;
    private static int FLIGHT_PROXY_INDEX = 1;
    private static int ROOM_PROXY_INDEX = 2;

    private LockManager lm = new LockManager();

    public TransactionManagerImpl(MiddleWareImpl middleware,
                                  middleware.ResourceManager carProxy,
                                  middleware.ResourceManager flightProxy,
                                  middleware.ResourceManager roomProxy ) {
        this.middleware = middleware;
        this.carProxy = carProxy;
        this.flightProxy = flightProxy;
        this.roomProxy = roomProxy;

        transactions = new ArrayList<Transaction>();
    }

    public void start(int id) {
        this.transactions.add(new Transaction(id));
        this.activeTransactions.put(id, new ArrayList<Integer>());
    }

    public void commit(int id) {
        //Unlock all
        this.lm.UnlockAll(id);
        //remove and place into active transactions
        this.transactions.remove(id);

        for (Integer rm : activeTransactions.get(id)) {
            commitToRM(id, rm);
        }
    }

    public void abort(int id) {
        //Unlock all
        this.lm.UnlockAll(id);

        //undo the operations on customer
        Transaction transaction = this.transactions.get(id);
        for (Operation op : transaction.history()) {
            middleware.undo(transaction.getId(), op);
        }
        this.transactions.remove(id);

        for (Integer rm : activeTransactions.get(id)) {
            abortToRM(id, rm);
        }
    }

    private void commitToRM(int id, int rmIndex) {
        if (rmIndex == CAR_PROXY_INDEX) {
            carProxy.commit(id);
        } else if (rmIndex == FLIGHT_PROXY_INDEX) {
            flightProxy.commit(id);
        } else {
            roomProxy.commit(id);
        }
    }

    private void abortToRM(int id, int rmIndex) {
        if (rmIndex == CAR_PROXY_INDEX) {
            carProxy.abort(id);
        } else if (rmIndex == FLIGHT_PROXY_INDEX) {
            flightProxy.abort(id);
        } else {
            roomProxy.abort(id);
        }
    }

    public void enlist(int id, int rmIndex) {
        //add operation to transaction with Id
        List<Integer> proxies = activeTransactions.get(id);
        if (!proxies.contains(rmIndex)) {
            proxies.add(rmIndex);
        }

    }
}
