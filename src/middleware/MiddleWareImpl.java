package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;
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

    private TransactionManager transactionManager;
    private LockManager lockManager;

    public MiddleWareImpl() {
//        String hosts[] = {"142.157.165.20","142.157.165.20","142.157.165.113","142.157.165.113" };
        String hosts[] = {"localhost","localhost","localhost" };
        int[] ports = {4000,4001,4002};

        setup(hosts, ports);
    }

    public MiddleWareImpl(String[] hosts, int[] ports) {
        if (ports.length != hosts.length) {
            System.out.println("Ports array length doesn't match hosts array length");
            return;
        }

        setup(hosts, ports);
    }

    public void setup(String[] hosts, int[] ports)
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

        // Setup TM and LM
        transactionManager = new TransactionManager(this, getCarProxy(), getFlightProxy(), getRoomProxy());
        lockManager = new LockManager();
    }


    private middleware.ResourceManager getCarProxy() { return proxy[MiddleWareImpl.CAR_PROXY_INDEX]; }
    private middleware.ResourceManager getFlightProxy() { return proxy[MiddleWareImpl.FLIGHT_PROXY_INDEX]; }
    private middleware.ResourceManager getRoomProxy() { return proxy[MiddleWareImpl.ROOM_PROXY_INDEX]; }

    // CUSTOMER

    protected RMHashtable m_itemHT = new RMHashtable();

    // Read a data item.
    private RMItem readData(int id, String key) {
        synchronized(m_itemHT) {
            return (RMItem) m_itemHT.get(key);
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
    protected RMItem removeData(int id, String key, RMItem oldValue) {
        synchronized(m_itemHT) {
            Transaction t = this.transactionManager.getTransaction(id);
            if (t != null)
                this.transactionManager.getTransaction(id).addOperation(new Operation(key, oldValue, DELETE));

            return (RMItem) m_itemHT.remove(key);
        }
    }

    @Override
    public int start() {
        return transactionManager.start();
    }

    @Override
    public boolean commit(int id) {
        // Check if transaction exists
        if (!transactionManager.transactionExists(id)) {
            Trace.error("Transaction " + id + " doesn't exist.");
            return false;
        }
        lockManager.UnlockAll(id);
        return transactionManager.commit(id);
    }

    @Override
    public boolean abort(int id) {
        // Check if transaction exists
        // Check if transaction exists
        if (!transactionManager.transactionExists(id)) {
            Trace.error("Transaction " + id + " doesn't exist.");
            return false;
        }
        lockManager.UnlockAll(id);
        return transactionManager.abort(id);
    }

    // Undo `operation`
    public void undo(int id, Operation operation) {
        // note id=-1 so that the operation won't be saved
        if (operation.isAdd())
            removeData(-1, operation.getKey(), null);
        else if (operation.isOvewrite() || operation.isDelete())
            writeData(-1, operation.getKey(), null, operation.getItem());
    }


    private boolean getLock(int id, String data, int type) {
        // Check if transaction exists
        if (!transactionManager.transactionExists(id)) {
            Trace.error("Transaction " + id + " doesn't exist.");
            return false;
        }

        // Try to acquire a lock
        try {
            return lockManager.Lock(id, data, type);
        } catch (DeadlockException e) {
            Trace.warn("Deadlock");
            lockManager.UnlockAll(id);
            transactionManager.abort(id);
        }
        return false;
    }

    @Override
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        if (getLock(id, "flight-" + Integer.toString(flightNumber), LockManager.WRITE)) {
            transactionManager.enlist(id, FLIGHT_PROXY_INDEX);
            return getFlightProxy().addFlight(id, flightNumber, numSeats, flightPrice);
        }
        return false;
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        if (getLock(id, "flight-" + Integer.toString(flightNumber), LockManager.WRITE)) {
            transactionManager.enlist(id, FLIGHT_PROXY_INDEX);
            return getFlightProxy().deleteFlight(id, flightNumber);
        }
        return false;
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        if (getLock(id, "flight-" + Integer.toString(flightNumber), LockManager.READ)) {
            return getFlightProxy().queryFlight(id, flightNumber);
        }
        return 0;
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        if (getLock(id, "flight-" + Integer.toString(flightNumber), LockManager.READ)) {
            return getFlightProxy().queryFlightPrice(id, flightNumber);
        }
        return 0;
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        if (getLock(id, "car-" + location, LockManager.WRITE)) {
            transactionManager.enlist(id, CAR_PROXY_INDEX);
            return getCarProxy().addCars(id, location, numCars, carPrice);
        }
        return false;
    }

    @Override
    public boolean deleteCars(int id, String location) {
        if (getLock(id, "car-" + location, LockManager.WRITE)) {
            transactionManager.enlist(id, CAR_PROXY_INDEX);
            return getCarProxy().deleteCars(id, location);
        }
        return false;
    }

    @Override
    public int queryCars(int id, String location) {
        if (getLock(id, "car-" + location, LockManager.READ)) {
            return getCarProxy().queryCars(id, location);
        }
        return 0;
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        if (getLock(id, "car-" + location, LockManager.READ)) {
            return getCarProxy().queryCarsPrice(id, location);
        }
        return 0;
    }


    @Override
    public boolean addRooms(int id, String location, int numRooms, int carPrice) {
        if (getLock(id, "car-" + location, LockManager.WRITE)) {
            transactionManager.enlist(id, CAR_PROXY_INDEX);
            return getRoomProxy().addRooms(id, location, numRooms, carPrice);
        }
        return false;
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        if (getLock(id, "car-" + location, LockManager.WRITE)) {
            transactionManager.enlist(id, CAR_PROXY_INDEX);
            return getRoomProxy().deleteRooms(id, location);
        }
        return false;
    }

    @Override
    public int queryRooms(int id, String location) {
        if (getLock(id, "car-" + location, LockManager.READ)) {
            return getRoomProxy().queryRooms(id, location);
        }
        return 0;
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
        if (getLock(id, "car-" + location, LockManager.READ)) {
            return getRoomProxy().queryRoomsPrice(id, location);
        }
        return 0;
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

            // Get a lock on the new customer object
            if (getLock(id, cust.getKey(), LockManager.WRITE)) {
                writeData(id, cust.getKey(), null, cust);
            }

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

        if (getLock(id, Customer.getKey(customerId), LockManager.READ)) {
            Customer cust = (Customer) readData(id, Customer.getKey(customerId));
            if (cust == null) {
                Trace.warn("RM::deleteCustomer(" + id + ", "
                        + customerId + ") failed: customer doesn't exist.");
                return false;
            } else {
                if (getLock(id, cust.getKey(), lockManager.WRITE)) {
                    // Remove the customer from the storage.
                    removeData(id, cust.getKey(), cust);
                    Trace.info("RM::deleteCustomer(" + id + ", " + customerId + ") OK.");
                    return true;
                }
            }
        }

        return false;
    }

    private boolean customerExists(int id, int customerId) {
        if (!getLock(id, Customer.getKey(customerId), LockManager.READ)) return false;

        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        return (cust != null);
    }

    public void setCustomerReservation(int id, int customerId, String key, String location, int price) {
        if (getLock(id, Customer.getKey(customerId), lockManager.WRITE)) {
            Customer cust = (Customer) readData(id, Customer.getKey(customerId));
            cust.reserve(key, location, price);
        }
    }

    // Return data structure containing customer reservation info.
    // Returns null if the customer doesn't exist.
    // Returns empty RMHashtable if customer exists but has no reservations.
    public Object[] getCustomerReservations(int id, int customerId) {
        Trace.info("RM::getCustomerReservations(" + id + ", "
                + customerId + ") called.");
        if (getLock(id, Customer.getKey(customerId), LockManager.READ)) {
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

        return new Object[]{};
    }

    // Return a bill.
    @Override
    public String queryCustomerInfo(int id, int customerId) {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + ") called.");
        if (getLock(id, Customer.getKey(customerId), LockManager.READ)) {
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

        return "";
    }

    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        if (!customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Flight.getKey(flightNumber) + ", " + flightNumber + ") failed: customer doesn't exist.");
            return false;
        }

        // Try to get a write lock on flight object
        if (!getLock(id, "flight-" + Integer.toString(flightNumber), LockManager.WRITE)) return false;

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

        // Try to get a write lock on flight object
        if (!getLock(id, "car-"+location, LockManager.WRITE)) return false;

        boolean reserved = getCarProxy().reserveCar(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Car.getKey(location) + ", " + location + ") failed: car cannot be reserved.");
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

        // Try to get a write lock on flight object
        if (!getLock(id, "room-"+location, LockManager.WRITE)) return false;

        boolean reserved = getRoomProxy().reserveRoom(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Room.getKey(location) + ", " + location + ") failed: room cannot be reserved.");
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

    @Override
    public boolean shutdown() {
        // Check that no transaction are running.
        if (transactionManager.isActive()) return false;

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

