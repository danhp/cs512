// -------------------------------
// Adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package server;

import server.ws.ResourceManager;

import java.util.*;
import javax.jws.WebService;

@WebService(endpointInterface = "server.ws.Middleware")
public class MiddlewareImpl implements server.ws.Middleware {

    protected RMHashtable m_itemHT = new RMHashtable();

    protected Map<RMType, ResourceManager> rmMap = new HashMap<RMType, ResourceManager>();

    public MiddlewareImpl() {
        rmMap.put(RMType.Car, new ResourceManagerImpl());
        rmMap.put(RMType.Flight, new ResourceManagerImpl());
        rmMap.put(RMType.Room, new ResourceManagerImpl());
    }

//     Basic operations on ReservableItem //

    private ReservableItem getItem(int id, RMType type, String key) {
        return rmMap.get(type).getItem(id, key);
    }

    private boolean addItem(int id, RMType type, ReservableItem item) {
        Trace.info("RM::addFlight(" + id + ", " + item.getKey()
                + ", $" + item.getPrice() + ", " + item.getCount() + ") called.");

        // Get the RM in charge of flights
        boolean result = rmMap.get(type).addItem(id, item);

        if (result) {
            Trace.info("RM::add" + type.name() + (" + id + ", " + item.getKey()"
                    + ", $" + item.getPrice() + ", " + item.getCount() + ") OK.");
        } else {
            Trace.warn("RM::add" + type.name() + (" + id + ", " + item.getKey()"
                    + ", $" + item.getPrice() + ", " + item.getCount() + ") FAILED.");
        }

        return result;
    }

//     Delete the entire item.
    protected boolean deleteItem(int id, RMType type, String key) {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called.");

        boolean result = rmMap.get(type).removeItem(id, key);
        if (result) {
            Trace.info("RM::deleteItem(" + id + ", " + key + ") OK.");
        } else {
            Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed: "
                    + " item doesn't exist."
                    + "some customers have reserved it.");
        }

        return result;

    }

    // Query the number of available seats/rooms/cars.
    protected int queryNum(int id, RMType type, String key) {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called.");

        ReservableItem item = getItem(id, type, key);

        int value = 0;
        if (item != null) {
            value = item.getCount();
        }
        Trace.info("RM::queryNum(" + id + ", " + key + ") OK: " + value);
        return value;
    }

    // Query the price of an item.
    protected int queryPrice(int id, RMType type, String key) {
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") called.");
        ReservableItem item = getItem(id, type, key);
        int value = 0;
        if (item != null) {
            value = item.getPrice();
        }
        Trace.info("RM::queryCarsPrice(" + id + ", " + key + ") OK: $" + value);
        return value;
    }

    // Reserve an item.
    protected boolean reserveItem(int id, int customerId,
                                  String key, String location) {
        Trace.info("RM::reserveItem(" + id + ", " + customerId + ", "
                + key + ", " + location + ") called.");
        // Read customer object if it exists (and read lock it).
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                   + key + ", " + location + ") failed: customer doesn't exist.");
            return false;
        }

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
            // Do reservation.
            cust.reserve(key, location, item.getPrice());
            writeData(id, cust.getKey(), cust);

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
        return addItem(id, RMType.Flight, new Flight(flightNumber, numSeats, flightPrice));
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        return deleteItem(id, RMType.Flight, Flight.getKey(flightNumber));
    }

    // Returns the number of empty seats on this flight.
    @Override
    public int queryFlight(int id, int flightNumber) {
        ReservableItem flight = getItem(id, RMType.Flight, Flight.getKey(flightNumber));
        return flight.getCount() - flight.getReserved();
    }

    // Returns price of this flight.
    public int queryFlightPrice(int id, int flightNumber) {
        return queryPrice(id, RMType.Flight, Flight.getKey(flightNumber));
    }

    // Car operations //

    // Create a new car, or add seats to existing car.
    // Note: if carPrice <= 0 and the car already exists, it maintains
    // its current price.
    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        return addItem(id, RMType.Car, new Car(location, numCars, carPrice));
    }

    @Override
    public boolean deleteCars(int id, String location) {
        return deleteItem(id, RMType.Car, Car.getKey(location));
    }

    // Returns the number of empty seats on this car.
    @Override
    public int queryCars(int id, String location) {
        ReservableItem car = getItem(id, RMType.Car, Car.getKey(location));
        return car.getCount() - car.getReserved();
    }

    // Returns price of this car.
    public int queryCarsPrice(int id, String location) {
        return queryPrice(id, RMType.Car, Car.getKey(location));
    }

    // Room operations //

    // Create a new room, or add seats to existing room.
    // Note: if roomPrice <= 0 and the room already exists, it maintains
    // its current price.
    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        return addItem(id, RMType.Room, new Room(location, numRooms, roomPrice));
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        return deleteItem(id, RMType.Room, Room.getKey(location));
    }

    // Returns the number of empty seats on this room.
    @Override
    public int queryRooms(int id, String location) {
        ReservableItem room = getItem(id, RMType.Room, Room.getKey(location));
        return room.getCount() - room.getReserved();
    }

    // Returns price of this room.
    public int queryRoomsPrice(int id, String location) {
        return queryPrice(id, RMType.Room, Room.getKey(location));
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

    // Customer operations //

    @Override
    public int newCustomer(int id) {
        Trace.info("INFO: RM::newCustomer(" + id + ") called.");
        // Generate a globally unique Id for the new customer.
        int customerId = Integer.parseInt(String.valueOf(id) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
        Customer cust = new Customer(customerId);
        writeData(id, cust.getKey(), cust);
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
            writeData(id, cust.getKey(), cust);
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
            // Increase the reserved numbers of all reservable items that
            // the customer reserved.
            RMHashtable reservationHT = cust.getReservations();
            for (Enumeration e = reservationHT.keys(); e.hasMoreElements();) {
                String reservedKey = (String) (e.nextElement());
                ReservedItem reservedItem = cust.getReservedItem(reservedKey);
                Trace.info("RM::deleteCustomer(" + id + ", " + customerId + "): "
                        + "deleting " + reservedItem.getCount() + " reservations "
                        + "for item " + reservedItem.getKey());
                ReservableItem item =
                        (ReservableItem) readData(id, reservedItem.getKey());
                item.setReserved(item.getReserved() - reservedItem.getCount());
                item.setCount(item.getCount() + reservedItem.getCount());
                Trace.info("RM::deleteCustomer(" + id + ", " + customerId + "): "
                        + reservedItem.getKey() + " reserved/available = "
                        + item.getReserved() + "/" + item.getCount());
            }
            // Remove the customer from the storage.
            removeData(id, cust.getKey());
            Trace.info("RM::deleteCustomer(" + id + ", " + customerId + ") OK.");
            return true;
        }
    }

    // Return data structure containing customer reservation info.
    // Returns null if the customer doesn't exist.
    // Returns empty RMHashtable if customer exists but has no reservations.
    public RMHashtable getCustomerReservations(int id, int customerId) {
        Trace.info("RM::getCustomerReservations(" + id + ", "
                + customerId + ") called.");
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.info("RM::getCustomerReservations(" + id + ", "
                    + customerId + ") failed: customer doesn't exist.");
            return null;
        } else {
            return cust.getReservations();
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


    // Reserve an itinerary.
    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers,
                                    String location, boolean car, boolean room) {
        return false;
    }

}
