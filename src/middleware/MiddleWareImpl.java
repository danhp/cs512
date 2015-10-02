package middleware;

import javax.jws.WebService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.net.URL;

import client.TCPMessage;
import middleware.ws.MiddleWare;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class MiddleWareImpl implements MiddleWare {

    private ServerSocket serverSocket;

    public MiddleWareImpl() {
        try{
            serverSocket = new ServerSocket(8000);
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
                                System.out.println("Received acknowledgement");
                                output.writeObject(ackPacket);
                            }

                            while (true) {
                                TCPMessage request = (TCPMessage) input.readObject();

                                // Do the request
//                                TCPMessage answer = encode(decode(request));
                                System.out.println("Decoding");

                                output.writeObject(ackPacket);
                            }
                        } catch (IOException ex) {
                        } catch (Exception ex) {
                            System.out.println(ex);
                        }
                    }
                });
            }
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
    }

    public Map<String, Object> addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        System.out.print("a");

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

    private TCPMessage decode(TCPMessage msg) {
        if (msg.type == 0) return new TCPMessage();

        Map<String, Object> map;

        switch (msg.itemType) {
            case 0:
                switch (msg.actionType) {
                    case 0:
                        map = getFlight(msg.id, Integer.parseInt(msg.key));
                        break;
                    case 1:
                        map = addFlight(msg.id, Integer.parseInt(msg.key), msg.count, msg.price);
                        break;
                    case 2:
                        map = deleteFlight(msg.id, Integer.parseInt(msg.key));
                        break;
                    case 3:
                        map = reserveFlight(msg.id, msg.customerId, Integer.parseInt(msg.key));
                        break;
                }
                break;
            case 1:
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

        return null;
    }

    public TCPMessage encode(Map<String, Object> dict) {
        TCPMessage msg = new TCPMessage();

        msg.id = (int)(dict.get("id") == null ? dict.get("id") : 0);
        msg.type = (int)(dict.get("type") == null ? dict.get("type") : 0);
        msg.itemType = (int)(dict.get("itemType") == null ? dict.get("itemType") : 0);
        msg.actionType = (int)(dict.get("actionType") == null ? dict.get("actionType") : 0);
        msg.key = (String)(dict.get("key") == null ? dict.get("key") : "");
        msg.count = (int)(dict.get("count") == null ? dict.get("count") : 0);
        msg.count2 = (int)(dict.get("count2") == null ? dict.get("count2") : 0);
        msg.car = (boolean)(dict.get("car") == null ? dict.get("car") : true);
        msg.room = (boolean)(dict.get("room") == null ? dict.get("room") : true);
        msg.customerId = (int)(dict.get("customerId") == null ? dict.get("customerId") : 0);
        msg.success = (boolean)(dict.get("success") == null ? dict.get("success") : true);

        return msg;
    }
}