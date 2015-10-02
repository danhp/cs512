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
    private int numberOfInitialAcks = 0;

    // streams
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public TCPClient(String host, int port) {
        System.out.println("Connecting to " + host + ":" + port);

        try {
            clientSocket = new Socket(host, port);

            output = new ObjectOutputStream(clientSocket.getOutputStream());
            input = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                TCPMessage responsePacket = (TCPMessage) input.readObject();

                if (responsePacket.type == 0) {
                    // first hello
                    if (numberOfInitialAcks < 1) {
                        // Answer with HELLO
                        TCPMessage answerPacket = new TCPMessage();
                        answerPacket.type = 0;

                        output.writeObject(answerPacket);

                        numberOfInitialAcks++;
                    } else {
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
            System.out.print("AddFlight: " + ret);

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
            ret = msg.price;

        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        } finally {
            return ret;
        }
    }

    // Car operations //

    /* Add seats to a car.
     * In general, this will be used to create a new car, but it should be
     * possible to add seats to an existing car.  Adding to an existing
     * car should overwrite the current price of the available seats.
     *
     * @return success.
     */
    public boolean addCars(int id, String location, int numSeats, int carPrice) {
        boolean ret = false;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 1;
            requestPacket.key = String.valueOf(location);
            requestPacket.count = numSeats;
            requestPacket.price = carPrice;

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
     * Delete the entire car.
     * This implies deletion of this car and all its seats.  If there is a
     * reservation on the car, then the car cannot be deleted.
     *
     * @return success.
     */
    public boolean deleteCars(int id, String location) {
        boolean ret = false;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 2;
            requestPacket.key = String.valueOf(location);

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

    /* Return the number of empty seats in this car. */
    public int queryCars(int id, String location) {
        int ret = 0;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 0;
            requestPacket.key = String.valueOf(location);

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

    /* Return the price of a seat on this car. */
    public int queryCarsPrice(int id, String location) {
        int ret = 0;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 0;
            requestPacket.key = String.valueOf(location);

            output.writeObject(requestPacket);

            // Wait for answer
            TCPMessage msg = (TCPMessage) input.readObject();
            ret = msg.price;

        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        } finally {
            return ret;
        }
    }


    // Room operations //

    /* Add seats to a room.
     * In general, this will be used to create a new room, but it should be
     * possible to add seats to an existing room.  Adding to an existing
     * room should overwrite the current price of the available seats.
     *
     * @return success.
     */
    public boolean addRooms(int id, String location, int numSeats, int roomPrice) {
        boolean ret = false;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 1;
            requestPacket.key = String.valueOf(location);
            requestPacket.count = numSeats;
            requestPacket.price = roomPrice;

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
     * Delete the entire room.
     * This implies deletion of this room and all its seats.  If there is a
     * reservation on the room, then the room cannot be deleted.
     *
     * @return success.
     */
    public boolean deleteRooms(int id, String location) {
        boolean ret = false;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 2;
            requestPacket.key = String.valueOf(location);

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

    /* Return the number of empty seats in this room. */
    public int queryRooms(int id, String location) {
        int ret = 0;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 0;
            requestPacket.key = String.valueOf(location);

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

    /* Return the price of a seat on this room. */
    public int queryRoomsPrice(int id, String location) {
        int ret = 0;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 0;
            requestPacket.key = String.valueOf(location);

            output.writeObject(requestPacket);

            // Wait for answer
            TCPMessage msg = (TCPMessage) input.readObject();
            ret = msg.price;

        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        } finally {
            return ret;
        }
    }
    // Customer operations //

    /* Create a new customer and return their unique identifier. */
    public int newCustomer(int id) {
        return 0;
    }

    /* Create a new customer with the provided identifier. */
    public boolean newCustomerId(int id, int customerId) {
        return true;
    }

    /* Remove this customer and all their associated reservations. */
    public boolean deleteCustomer(int id, int customerId) {
        return true;
    }

    /* Return a bill. */
    public String queryCustomerInfo(int id, int customerId) {

        return "";
    }

    /* Reserve a seat on this flight. */
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        boolean ret = false;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 1;
            requestPacket.actionType = 3;
            requestPacket.key = String.valueOf(flightNumber);
            requestPacket.customerId = customerId;

            output.writeObject(requestPacket);

            // Wait for answer.
            TCPMessage msg = (TCPMessage) input.readObject();
            ret = msg.success;

        } catch (IOException e) {
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        } finally {
            return ret;
        }
    }

    /* Reserve a car at this location. */
    public boolean reserveCar(int id, int customerId, String location) {
        boolean ret = false;
        try {
            TCPMessage requestPacket = new TCPMessage();
            requestPacket.id = id;
            requestPacket.type = 1;
            requestPacket.itemType = 0;
            requestPacket.actionType = 3;
            requestPacket.key = location;
            requestPacket.customerId = customerId;

            output.writeObject(requestPacket);

            // Wait for answer.
            TCPMessage msg = (TCPMessage) input.readObject();
            ret = msg.success;

        } catch (IOException e) {
            System.out.println(e);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
        } finally {
            return ret;
        }
    }

    /* Reserve a room at this location. */
    public boolean reserveRoom(int id, int customerId, String location) {
        return true;
    }

    /* Reserve an itinerary. */
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers,
                                    String location, boolean car, boolean room) {
        return true;
    }
}
