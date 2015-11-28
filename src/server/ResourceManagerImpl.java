// -------------------------------
// Adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package server;

import middleware.TMData;
import utils.Constants;
import utils.Constants.TransactionStatus;
import utils.Storage;

import javax.jws.WebService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@WebService(endpointInterface = "server.ws.ResourceManager")
public class ResourceManagerImpl implements server.ws.ResourceManager {

    protected RMHashtable m_itemHT;

    private Map<Integer, Map<String, RMItem>> readSet = new ConcurrentHashMap<>();
    private Map<Integer, Map<String, RMItem>> writeSet = new ConcurrentHashMap<>();
    private Map<Integer, TransactionStatus> status = new ConcurrentHashMap<>();

    //constructor to allow potential recovery of RM
    //TO DO: Rm knows and understands what type of RM it is
    public ResourceManagerImpl() {

        // Recover if a file is found for RM.
        try {
            RMData data = (RMData) Storage.get(Constants.CAR_FILE);
            System.out.println("Recovering from RM Data within file");
            this.m_itemHT = data.getData();
            this.readSet = data.getReadSet();
            this.writeSet = data.getWriteSet();
            this.status = data.getStatus();

        } catch (Exception ex) {
            System.out.println("File either not found or corrupted\nStarting new RM");
            this.m_itemHT = new RMHashtable();
            this.readSet = new ConcurrentHashMap<>();
            this.writeSet = new ConcurrentHashMap<>();
            this.status = new ConcurrentHashMap<();
        }
    }


    // Basic operations on RMItem //

    // Read a data item.
    private RMItem readData(int transactionID, String key) {
        synchronized(m_itemHT) {
            // Check the writeSet
            if (writeSet.get(transactionID).containsKey(key))
                return writeSet.get(transactionID).get(key);

            // Check the readSet
            if (readSet.get(transactionID).containsKey(key))
                return readSet.get(transactionID).get(key);

            // Else get data from database
            RMItem item = (RMItem) m_itemHT.get(key);
            this.readSet.get(transactionID).put(key, item);
            return item;
        }
    }

    // Write a data item.
    private void writeData(int transactionID, String key, RMItem newValue, boolean commit) {
        synchronized(m_itemHT) {
            if (commit) {
                m_itemHT.put(key, newValue);
            } else {
                // save the data in the write set.
                this.writeSet.get(transactionID).put(key, newValue);

                // save the data in the read set if not present
                RMItem item = this.readSet.get(transactionID).get(key);
                if (item == null) {
                    item = (RMItem) m_itemHT.get(key);
                    this.readSet.get(transactionID).put(key, item);
                }
            }
        }
    }

    // Remove the item out of storage.
    protected void removeData(int transactionID, String key, boolean commit) {
        synchronized(m_itemHT) {
            if (commit) {
                m_itemHT.remove(key);
            } else {
                // save the data in the write set.
                // tag it with null to mark for deletion.
                this.writeSet.get(transactionID).put(key, null);
            }
        }
    }

    //saves data to RM file in case of crash
    private void save() {
        RMData toSave = new RMData(this.m_itemHT,
                this.readSet,
                this.writeSet,
                this.status);
        try {
            //save to storage based on type of RM
            Storage.set(toSave, Constants.CAR_FILE);
            System.out.println("saved car file");
        } catch (Exception ex) {
            System.out.println(ex);
            System.out.println("Failed to write to disk");
        }
    }


    // TRANSACTIONS
    @Override
    public void start(int transactionID) {
        synchronized (writeSet) {
            if (writeSet.containsKey(transactionID)) return;
            writeSet.put(transactionID, new HashMap<String, RMItem>());
        }
        synchronized (readSet) {
            readSet.put(transactionID, new HashMap<String, RMItem>());
        }

        this.save();
    }

    @Override
    public void doCommit(int transactionID) {
        synchronized (writeSet) {
            if (!writeSet.containsKey(transactionID)) {
                Trace.warn("Transaction " + transactionID + " does not exist. Ignoring.");
            }

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

    @Override
    public void doAbort(int transactionID) {
        synchronized (this.writeSet) {
            if (!writeSet.containsKey(transactionID)) return;
            this.writeSet.remove(transactionID);
        }
        synchronized (this.readSet) {
            this.readSet.remove(transactionID);
        }
    }

    @Override
    public void prepare(int transactionID) throws TransactionAbortedException, InvalidTransactionException {
        System.out.println("Preparing for transaction " + transactionID);
        synchronized (this.readSet) {
            if (!this.readSet.containsKey(transactionID)) { throw new InvalidTransactionException(); }

            for (Map.Entry<String, RMItem> entry : readSet.get(transactionID).entrySet()) {

                synchronized (m_itemHT) {
                    RMItem inDatabase = (RMItem) m_itemHT.get(entry.getKey());

                    if (inDatabase != null){
                        if (inDatabase.equals(entry.getValue()))
                            throw new InvalidTransactionException();
                    }
                }

            }
        }
        // Went through all the entries and they match.
    }

    @Override
    public boolean haveYouCommitted(int id) {
        boolean commitStatus = !readSet.containsKey(id);
        System.out.println("Returning commit status: " + commitStatus);
        return commitStatus;
    }

    @Override
    public void selfDestruct() {
        System.out.println("Initiating self destruct sequence...\n");
        System.exit(-1);
    }

    // Basic operations on ReservableItem //

    // Delete the entire item.
    protected boolean deleteItem(int id, String key) {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        // Check if there is such an item in the storage.
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed: "
                    + " item doesn't exist.");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(id, curObj.getKey(), false);
                Trace.info("RM::deleteItem(" + id + ", " + key + ") OK.");
                return true;
            }
            else {
                Trace.info("RM::deleteItem(" + id + ", " + key + ") failed: "
                        + "some customers have reserved it.");
                return false;
            }
        }
    }

    // Called to abort a set of operations.
    // Only affect the write set.
    @Override
    public void unreserveItem(int id, String key, String location, int count) {
        Trace.info("RM::unreserveItem(" + id + ", " + key + ") called.");

        synchronized (this.writeSet) {
            ReservableItem item = (ReservableItem) this.writeSet.get(id).get(key);
            if (item != null) {
                item.setReserved(item.getReserved() - count);
                item.setCount(item.getCount() + 1);
            }
        }
    }

    // Query the number of available seats/rooms/cars.
    protected int queryNum(int id, String key) {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + id + ", " + key + ") OK: " + value);
        return value;
    }

    // Query the price of an item.
    protected int queryPrice(int id, String key) {
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") OK: $" + value);
        return value;
    }

    // Reserve an item.
    protected boolean reserveItem(int id, int customerId,
                                  String key, String location) {
        Trace.info("RM::reserveItem(" + id + ", " + customerId + ", "
                + key + ", " + location + ") called.");

        // Check if the item is available.
        ReservableItem item = (ReservableItem) readData(id, key);
        if (item == null) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + key + ", " + location + ") failed: item doesn't exist.");
            return false;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + key + ", " + location + ") failed: no more items.");
            return false;
        } else {
            // Decrease the number of available items in the storage.
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);

            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + key + ", " + location + ") OK.");
            return true;
        }
    }


    // Flight operations //

    // Create a new flight, or add seats to existing flight.
    // Note: if flightPrice <= 0 and the flight already exists, it maintains
    // its current price.
    @Override
    public boolean addFlight(int id, int flightNumber,
                             int numSeats, int flightPrice) {
        Trace.info("RM::addFlight(" + id + ", " + flightNumber
                + ", $" + flightPrice + ", " + numSeats + ") called.");
        Flight curObj = (Flight) readData(id, Flight.getKey(flightNumber));
        if (curObj == null) {
            // Doesn't exist; add it.
            Flight newObj = new Flight(flightNumber, numSeats, flightPrice);
            writeData(id, newObj.getKey(), newObj, false);
            Trace.info("RM::addFlight(" + id + ", " + flightNumber
                    + ", $" + flightPrice + ", " + numSeats + ") OK.");
        } else {
            // Add seats to existing flight and update the price.
            curObj.setCount(curObj.getCount() + numSeats);
            if (flightPrice > 0) {
                curObj.setPrice(flightPrice);
            }
            writeData(id, curObj.getKey(), curObj, false);
            Trace.info("RM::addFlight(" + id + ", " + flightNumber
                    + ", $" + flightPrice + ", " + numSeats + ") OK: "
                    + "seats = " + curObj.getCount() + ", price = $" + flightPrice);
        }
        return(true);
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        return deleteItem(id, Flight.getKey(flightNumber));
    }

    // Returns the number of empty seats on this flight.
    @Override
    public int queryFlight(int id, int flightNumber) {
        return queryNum(id, Flight.getKey(flightNumber));
    }

    // Returns price of this flight.
    public int queryFlightPrice(int id, int flightNumber) {
        return queryPrice(id, Flight.getKey(flightNumber));
    }

    /*
    // Returns the number of reservations for this flight.
    public int queryFlightReservations(int id, int flightNumber) {
        Trace.info("RM::queryFlightReservations(" + id
                + ", #" + flightNumber + ") called.");
        RMInteger numReservations = (RMInteger) readData(id,
                Flight.getNumReservationsKey(flightNumber));
        if (numReservations == null) {
            numReservations = new RMInteger(0);
       }
        Trace.info("RM::queryFlightReservations(" + id +
                ", #" + flightNumber + ") = " + numReservations);
        return numReservations.getValue();
    }
    */

    /*
    // Frees flight reservation record. Flight reservation records help us
    // make sure we don't delete a flight if one or more customers are
    // holding reservations.
    public boolean freeFlightReservation(int id, int flightNumber) {
        Trace.info("RM::freeFlightReservations(" + id + ", "
                + flightNumber + ") called.");
        RMInteger numReservations = (RMInteger) readData(id,
                Flight.getNumReservationsKey(flightNumber));
        if (numReservations != null) {
            numReservations = new RMInteger(
                    Math.max(0, numReservations.getValue() - 1));
        }
        writeData(id, Flight.getNumReservationsKey(flightNumber), numReservations);
        Trace.info("RM::freeFlightReservations(" + id + ", "
                + flightNumber + ") OK: reservations = " + numReservations);
        return true;
    }
    */


    // Car operations //

    // Create a new car location or add cars to an existing location.
    // Note: if price <= 0 and the car location already exists, it maintains
    // its current price.
    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        Trace.info("RM::addCars(" + id + ", " + location + ", "
                + numCars + ", $" + carPrice + ") called.");
        Car curObj = (Car) readData(id, Car.getKey(location));
        if (curObj == null) {
            // Doesn't exist; add it.
            Car newObj = new Car(location, numCars, carPrice);
            writeData(id, newObj.getKey(), newObj, false);
            Trace.info("RM::addCars(" + id + ", " + location + ", "
                    + numCars + ", $" + carPrice + ") OK.");
        } else {
            // Add count to existing object and update price.
            curObj.setCount(curObj.getCount() + numCars);
            if (carPrice > 0) {
                curObj.setPrice(carPrice);
            }
            writeData(id, curObj.getKey(), curObj, false);
            Trace.info("RM::addCars(" + id + ", " + location + ", "
                    + numCars + ", $" + carPrice + ") OK: "
                    + "cars = " + curObj.getCount() + ", price = $" + carPrice);
        }
        return(true);
    }

    // Delete cars from a location.
    @Override
    public boolean deleteCars(int id, String location) {
        return deleteItem(id, Car.getKey(location));
    }

    // Returns the number of cars available at a location.
    @Override
    public int queryCars(int id, String location) {
        return queryNum(id, Car.getKey(location));
    }

    // Returns price of cars at this location.
    @Override
    public int queryCarsPrice(int id, String location) {
        return queryPrice(id, Car.getKey(location));
    }

    // Room operations //

    // Create a new room location or add rooms to an existing location.
    // Note: if price <= 0 and the room location already exists, it maintains
    // its current price.
    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        Trace.info("RM::addRooms(" + id + ", " + location + ", "
                + numRooms + ", $" + roomPrice + ") called.");
        Room curObj = (Room) readData(id, Room.getKey(location));
        if (curObj == null) {
            // Doesn't exist; add it.
            Room newObj = new Room(location, numRooms, roomPrice);
            writeData(id, newObj.getKey(), newObj, false);
            Trace.info("RM::addRooms(" + id + ", " + location + ", "
                    + numRooms + ", $" + roomPrice + ") OK.");
        } else {
            // Add count to existing object and update price.
            curObj.setCount(curObj.getCount() + numRooms);
            if (roomPrice > 0) {
                curObj.setPrice(roomPrice);
            }
            writeData(id, curObj.getKey(), curObj, false);
            Trace.info("RM::addRooms(" + id + ", " + location + ", "
                    + numRooms + ", $" + roomPrice + ") OK: "
                    + "rooms = " + curObj.getCount() + ", price = $" + roomPrice);
        }
        return(true);
    }

    // Delete rooms from a location.
    @Override
    public boolean deleteRooms(int id, String location) {
        return deleteItem(id, Room.getKey(location));
    }

    // Returns the number of rooms available at a location.
    @Override
    public int queryRooms(int id, String location) {
        return queryNum(id, Room.getKey(location));
    }

    // Returns room price at this location.
    @Override
    public int queryRoomsPrice(int id, String location) {
        return queryPrice(id, Room.getKey(location));
    }

    // Add flight reservation to this customer.
    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        return reserveItem(id, customerId,
                Flight.getKey(flightNumber), String.valueOf(flightNumber));
    }

    // Add car reservation to this customer.
    @Override
    public boolean reserveCar(int id, int customerId, String location) {
        return reserveItem(id, customerId, Car.getKey(location), location);
    }

    // Add room reservation to this customer.
    @Override
    public boolean reserveRoom(int id, int customerId, String location) {
        return reserveItem(id, customerId, Room.getKey(location), location);
    }

    // Assumes this being called when no transaction associated with it.
    @Override
    public boolean shutdown() {
        System.exit(0);
        return true;
    }
}
