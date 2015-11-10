package middleware.ws;

import javax.jws.WebMethod;
import javax.jws.WebService;
import java.util.Vector;

@WebService
public interface MiddleWare {
    // Transaction operations //
    @WebMethod
    public void start(int id);

    @WebMethod
    public void commit(int id);

    @WebMethod
    public void abort(int id);

    // Flight operations //

    /* Add seats to a flight.
     * In general, this will be used to create a new flight, but it should be
     * possible to add seats to an existing flight.  Adding to an existing
     * flight should overwrite the current price of the available seats.
     *
     * @return success.
     */
    @WebMethod
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice);

    /**
     * Delete the entire flight.
     * This implies deletion of this flight and all its seats.  If there is a
     * reservation on the flight, then the flight cannot be deleted.
     *
     * @return success.
     */
    @WebMethod
    public boolean deleteFlight(int id, int flightNumber);

    /* Return the number of empty seats in this flight. */
    @WebMethod
    public int queryFlight(int id, int flightNumber);

    /* Return the price of a seat on this flight. */
    @WebMethod
    public int queryFlightPrice(int id, int flightNumber);


    // Car operations //

    /* Add cars to a location.
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    @WebMethod
    public boolean addCars(int id, String location, int numCars, int carPrice);

    /* Delete all cars from a location.
     * It should not succeed if there are reservations for this location.
     */
    @WebMethod
    public boolean deleteCars(int id, String location);

    /* Return the number of cars available at this location. */
    @WebMethod
    public int queryCars(int id, String location);

    /* Return the price of a car at this location. */
    @WebMethod
    public int queryCarsPrice(int id, String location);


    // Room operations //

    /* Add rooms to a location.
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    @WebMethod
    public boolean addRooms(int id, String location, int numRooms, int roomPrice);

    /* Delete all rooms from a location.
     * It should not succeed if there are reservations for this location.
     */
    @WebMethod
    public boolean deleteRooms(int id, String location);

    /* Return the number of rooms available at this location. */
    @WebMethod
    public int queryRooms(int id, String location);

    /* Return the price of a room at this location. */
    @WebMethod
    public int queryRoomsPrice(int id, String location);


    // Customer operations //

    /* Create a new customer and return their unique identifier. */
    @WebMethod
    public int newCustomer(int id);

    /* Create a new customer with the provided identifier. */
    @WebMethod
    public boolean newCustomerId(int id, int customerId);

    /* Remove this customer and all their associated reservations. */
    @WebMethod
    public boolean deleteCustomer(int id, int customerId);

    /* Return a bill. */
    @WebMethod
    public String queryCustomerInfo(int id, int customerId);

    /* Reserve a seat on this flight. */
    @WebMethod
    public boolean reserveFlight(int id, int customerId, int flightNumber);

    /* Reserve a car at this location. */
    @WebMethod
    public boolean reserveCar(int id, int customerId, String location);

    /* Reserve a room at this location. */
    @WebMethod
    public boolean reserveRoom(int id, int customerId, String location);

    /* Reserve an itinerary. */
    @WebMethod
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers,
                                    String location, boolean car, boolean room);

    @WebMethod
    public boolean shutdown();
}