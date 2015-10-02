package middleware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import client.TCPMessage;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class MiddleWareImpl {

    private ServerSocket serverSocket;

    public MiddleWareImpl(int port) {
        System.out.println("Creating middleware at " + port);
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public void run() {
        System.out.println("Starting middleware.");
        try {
            while (true) {
                Socket serviceSocket = serverSocket.accept();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ObjectOutputStream output = new ObjectOutputStream(serviceSocket.getOutputStream());
                            ObjectInputStream input = new ObjectInputStream(serviceSocket.getInputStream());

                            TCPMessage ackPacket = new TCPMessage();
                            ackPacket.id = 0;
                            ackPacket.type = 0;
                            output.writeObject(ackPacket);

                            TCPMessage connectionPacket = (TCPMessage) input.readObject();

                            //if HELLO
                            if (connectionPacket.type == 0) {
                                System.out.println("Connection established.");
                                output.writeObject(ackPacket);
                            }

                            while (true) {
                                TCPMessage request = (TCPMessage) input.readObject();

                                // Do the request
                                TCPMessage answer = encode(decode(request));

                                output.writeObject(answer);
                            }
                        } catch (IOException ex) {
                            System.out.println(ex);
                        } catch (Exception ex) {
                            System.out.println(ex);
                        }
                    }
                }).run();
            }
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public Map<String, Object> addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("success", true);

        return ret;
    }

    public Map<String, Object> deleteFlight(int id, int flightNumber) {
        Map<String, Object> ret = new HashMap<String, Object>();
        return ret;
    }

    public Map<String, Object> queryFlight(int id, int flightNumber) {
        Map<String, Object> ret = new HashMap<String, Object>();
        return ret;
    }

    public Map<String, Object> addCars(int id, String location, int numCars, int carPrice) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> getFlight(int id, int flightNumber) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> deleteCars(int id, String location) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> queryCars(int id, String location) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> addRooms(int id, String location, int numRooms, int roomPrice) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> getCars(int id, String location) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> deleteRooms(int id, String location) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> queryRooms(int id, String location) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> newCustomer(int id) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> getRooms(int id, String location) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> newCustomerId(int id, int customerId) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> deleteCustomer(int id, int customerId) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> queryCustomerInfo(int id, int customerId) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> reserveFlight(int id, int customerId, int flightNumber) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> reserveCar(int id, int customerId, String location) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> reserveRoom(int id, int customerId, String location) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    public Map<String, Object> reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
        Map<String, Object> ret = new HashMap<>();
        return ret;
    }

    private Map<String, Object> decode(TCPMessage msg) {
        Map<String, Object> map = new HashMap<>();

        System.out.println("Type" + msg.type + " itemtype " + msg.itemType + " actiontype: + " + msg.actionType);
        if (msg.type == 0) return map;

        switch (msg.itemType) {
            case 1:
                // Dispatch to flight rm
                //
                break;
            case 0:
                // Dispatch to car
                switch (msg.actionType) {
                    case 0:
                        map = getCars(msg.id, msg.key);
                        break;
                    case 1:
                        map = addCars(msg.id, msg.key, msg.count, msg.price);
                        break;
                    case 2:
                        map = deleteCars(msg.id, msg.key);
                        break;
                    case 3:
                        map = reserveCar(msg.id, msg.customerId, msg.key);
                        break;
                }
                break;
            case 2:
                switch (msg.actionType) {
                    case 0:
                        map = getRooms(msg.id, msg.key);
                        break;
                    case 1:
                        map = addRooms(msg.id, msg.key, msg.count, msg.price);
                        break;
                    case 2:
                        map = deleteRooms(msg.id, msg.key);
                        break;
                    case 3:
                        map = reserveRoom(msg.id, msg.customerId, msg.key);
                        break;
                }
                break;
            case 3:
                throw new NotImplementedException();
            case 4:
                throw new NotImplementedException();
        }

        return map;
    }


}