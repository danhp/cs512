package middleware.ws;

import java.util.Map;
import java.util.Vector;

public interface MiddleWare {
    // Flight operations //

    /* Add seats to a flight.
     * In general, this will be used to create a new flight, but it should be
     * possible to add seats to an existing flight.  Adding to an existing
     * flight should overwrite the current price of the available seats.
     *
     * @return success.
     */
    public Map<String, Object> addFlight(int id, int flightNumber, int numSeats, int flightPrice);

    /**
     * Delete the entire flight.
     * This implies deletion of this flight and all its seats.  If there is a
     * reservation on the flight, then the flight cannot be deleted.
     *
     * @return success.
     */
    public Map<String, Object> deleteFlight(int id, int flightNumber);

    /* Return the number of empty seats in this flight. */
    public Map<String, Object> queryFlight(int id, int flightNumber);


    // Car operations //

    /* Add cars to a location.
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public Map<String, Object> addCars(int id, String location, int numCars, int carPrice);

    /* Delete all cars from a location.
     * It should not succeed if there are reservations for this location.
     */
    public Map<String, Object> deleteCars(int id, String location);

    /* Return the number of cars available at this location. */
    public Map<String, Object> queryCars(int id, String location);

    // Room operations //

    /* Add rooms to a location.
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public Map<String, Object> addRooms(int id, String location, int numRooms, int roomPrice);

    /* Delete all rooms from a location.
     * It should not succeed if there are reservations for this location.
     */
    public Map<String, Object> deleteRooms(int id, String location);

    /* Return the number of rooms available at this location. */
    public Map<String, Object> queryRooms(int id, String location);

    // Customer operations //

    /* Create a new customer and return their unique identifier. */
    public Map<String, Object> newCustomer(int id);

    /* Create a new customer with the provided identifier. */
    public Map<String, Object> newCustomerId(int id, int customerId);

    /* Remove this customer and all their associated reservations. */
    public Map<String, Object> deleteCustomer(int id, int customerId);

    /* Return a bill. */
    public Map<String, Object> queryCustomerInfo(int id, int customerId);

    /* Reserve a seat on this flight. */
    public Map<String, Object> reserveFlight(int id, int customerId, int flightNumber);

    /* Reserve a car at this location. */
    public Map<String, Object> reserveCar(int id, int customerId, String location);

    /* Reserve a room at this location. */
    public Map<String, Object> reserveRoom(int id, int customerId, String location);


    /* Reserve an itinerary. */
    public Map<String, Object> reserveItinerary(int id, int customerId, Vector flightNumbers,
                                    String location, boolean car, boolean room);

}
