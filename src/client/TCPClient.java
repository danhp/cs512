package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

/**
 * Created by justindomingue on 15-10-01.
 */
public class TCPClient {
    private Socket clientSocket;
    private boolean connectionEstablished = false;

    // streams
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public TCPClient() {
        try {
            clientSocket = new Socket("0.0.0.0", 8000);

            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                TCPMessage responsePacket = (TCPMessage) input.readObject();

                if (responsePacket.type == 0) {
                    // first hello
                    if (!connectionEstablished) {
                        // Answer with HELLO
                        TCPMessage answerPacket = new TCPMessage();
                        answerPacket.type = 0;

                        output.writeObject(answerPacket);
                    } else {
                        connectionEstablished = true;

                        System.out.println("Connection established.");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        }
    }

    // Flight operations //

    /* Add seats to a flight.
     * In general, this will be used to create a new flight, but it should be
     * possible to add seats to an existing flight.  Adding to an existing
     * flight should overwrite the current price of the available seats.
     *
     * @return success.
     */
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        boolean ret = false;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 1;
            requestPacket.actionType = 1;
            requestPacket.key = String.valueOf(flightNumber);
            requestPacket.count = numSeats;
            requestPacket.price = flightPrice;

            output.writeObject(requestPacket);

            // Wait for answer
            TCPMessage msg = (TCPMessage) input.readObject();
            ret = msg.success;

        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        } finally {
            return ret;
        }
    }

    /**
     * Delete the entire flight.
     * This implies deletion of this flight and all its seats.  If there is a
     * reservation on the flight, then the flight cannot be deleted.
     *
     * @return success.
     */
    public boolean deleteFlight(int id, int flightNumber) {
        boolean ret = false;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 1;
            requestPacket.actionType = 2;
            requestPacket.key = String.valueOf(flightNumber);

            output.writeObject(requestPacket);

            // Wait for answer
            TCPMessage msg = (TCPMessage) input.readObject();
            ret = msg.success;

        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        } finally {
            return ret;
        }
    }

    /* Return the number of empty seats in this flight. */
    public int queryFlight(int id, int flightNumber) {
        int ret = 0;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 1;
            requestPacket.actionType = 0;
            requestPacket.key = String.valueOf(flightNumber);

            output.writeObject(requestPacket);

            // Wait for answer
            TCPMessage msg = (TCPMessage) input.readObject();
            ret = msg.count - msg.count2;

        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        } finally {
            return ret;
        }
    }

    /* Return the price of a seat on this flight. */
    public int queryFlightPrice(int id, int flightNumber) {

    }


    // Car operations //

    /* Add cars to a location.
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public boolean addCars(int id, String location, int numCars, int carPrice) {

    }

    /* Delete all cars from a location.
     * It should not succeed if there are reservations for this location.
     */
    public boolean deleteCars(int id, String location) {

    }

    /* Return the number of cars available at this location. */
    public int queryCars(int id, String location) {

    }

    /* Return the price of a car at this location. */
    public int queryCarsPrice(int id, String location) {

    }


    // Room operations //

    /* Add rooms to a location.
     * This should look a lot like addFlight, only keyed on a string location
     * instead of a flight number.
     */
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {

    }

    /* Delete all rooms from a location.
     * It should not succeed if there are reservations for this location.
     */
    public boolean deleteRooms(int id, String location) {

    }

    /* Return the number of rooms available at this location. */
    public int queryRooms(int id, String location) {

    }

    /* Return the price of a room at this location. */
    public int queryRoomsPrice(int id, String location) {

    }


    // Customer operations //

    /* Create a new customer and return their unique identifier. */
    public int newCustomer(int id) {

    }

    /* Create a new customer with the provided identifier. */
    public boolean newCustomerId(int id, int customerId) {

    }

    /* Remove this customer and all their associated reservations. */
    public boolean deleteCustomer(int id, int customerId) {

    }

    /* Return a bill. */
    public String queryCustomerInfo(int id, int customerId) {

    }

    /* Reserve a seat on this flight. */
    public boolean reserveFlight(int id, int customerId, int flightNumber) {

    }

    /* Reserve a car at this location. */
    public boolean reserveCar(int id, int customerId, String location) {

    }

    /* Reserve a room at this location. */
    public boolean reserveRoom(int id, int customerId, String location) {

    }

    /* Reserve an itinerary. */
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers,
                                    String location, boolean car, boolean room) {

    }
}
