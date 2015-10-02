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

    private Socket[] rmSockets = new Socket[3];

    public MiddleWareImpl(int port,
                          String carHost, int carPort,
                          String flightHost, int flightPort,
                          String roomHost, int roomPort) {
        try {
            System.out.println("Creating middleware at " + port);
            serverSocket = new ServerSocket(port);

            System.out.println("Connecting to the resource manager ");
            rmSockets[0] = new Socket(carHost, carPort);
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try {
            rmSockets[1] = new Socket(flightHost, flightPort);
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try{
            rmSockets[2] = new Socket(roomHost, roomPort);
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    public void startMiddlware() {
        System.out.println("Starting middleware.");
        try {
            while (true) {
                Socket socket = serverSocket.accept();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

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
                                TCPMessage answer = decode(request);

                                System.out.println("Received answer  " + answer);
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

    private TCPMessage forward(int index, TCPMessage msg) {
        Socket socket = rmSockets[index];
        TCPMessage incoming = null;

        try {
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

            output.writeObject(msg);
            incoming = (TCPMessage) input.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            return incoming;
        }


    }
//
//    public Map<String, Object> addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
//        Map<String, Object> ret = new HashMap<String, Object>();
//        ret.put("success", true);
//
//        return ret;
//    }
//
//    public Map<String, Object> deleteFlight(int id, int flightNumber) {
//        Map<String, Object> ret = new HashMap<String, Object>();
//        return ret;
//    }
//
//    public Map<String, Object> queryFlight(int id, int flightNumber) {
//        Map<String, Object> ret = new HashMap<String, Object>();
//        return ret;
//    }
//
//    public Map<String, Object> addCars(int id, String location, int numCars, int carPrice) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> getFlight(int id, int flightNumber) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> deleteCars(int id, String location) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> queryCars(int id, String location) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> addRooms(int id, String location, int numRooms, int roomPrice) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> getCars(int id, String location) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> deleteRooms(int id, String location) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> queryRooms(int id, String location) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> newCustomer(int id) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> getRooms(int id, String location) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> newCustomerId(int id, int customerId) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> deleteCustomer(int id, int customerId) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> queryCustomerInfo(int id, int customerId) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> reserveFlight(int id, int customerId, int flightNumber) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> reserveCar(int id, int customerId, String location) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> reserveRoom(int id, int customerId, String location) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }
//
//    public Map<String, Object> reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
//        Map<String, Object> ret = new HashMap<>();
//        return ret;
//    }

    private TCPMessage decode(TCPMessage msg) {
        Map<String, Object> map = new HashMap<>();

        System.out.println("Type" + msg.type + " itemtype " + msg.itemType + " actiontype: + " + msg.actionType);
        if (msg.type == 0) return msg;

        return forward(msg.itemType, msg);
    }
}