package middleware;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLSyntaxErrorException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import client.TCPMessage;

public class MiddleWareImpl {

    private ServerSocket serverSocket;

    private Socket[] rmSockets = new Socket[3];
    private ObjectInputStream[] inputs = new ObjectInputStream[3];
    private ObjectOutputStream[] outputs = new ObjectOutputStream[3];

    // Customer Objects
    private Map<Integer, Customer> map = new HashMap<Integer, Customer>();

    public MiddleWareImpl(int port,
                          String carHost, int carPort,
                          String flightHost, int flightPort,
                          String roomHost, int roomPort) {
        try {
            System.out.println("Creating middleware at " + port);
            serverSocket = new ServerSocket(port);

            System.out.println("Connecting to the Car resource manager ");
            rmSockets[0] = new Socket(carHost, carPort);
            outputs[0] = new ObjectOutputStream(rmSockets[0].getOutputStream());
            inputs[0] = new ObjectInputStream(rmSockets[0].getInputStream());

        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try {
            System.out.println("Connecting to the Flight resource manager ");
            rmSockets[1] = new Socket(flightHost, flightPort);
            outputs[1] = new ObjectOutputStream(rmSockets[1].getOutputStream());
            inputs[1] = new ObjectInputStream(rmSockets[1].getInputStream());

        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try{
            System.out.println("Connecting to the Room resource manager");
            rmSockets[2] = new Socket(roomHost, roomPort);
            outputs[2] = new ObjectOutputStream(rmSockets[2].getOutputStream());
            inputs[2] = new ObjectInputStream(rmSockets[2].getInputStream());

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

    private TCPMessage decode(TCPMessage msg) {
        Map<String, Object> map = new HashMap<>();

        System.out.println("Type " + msg.type + " ,itemtype " + msg.itemType + " ,actiontype: + " + msg.actionType);
        if (msg.type == 0) return msg;

        switch (msg.itemType){
            case 3:
                System.out.println("Handling customer");
                return handleCustomer(msg);
            case 4:
                return itinerary(msg);
            default:
                // Process info if attempting to reserve
                TCPMessage answer = new TCPMessage();

                if (msg.actionType == 3) {
                    // Customer not found,
                    if (!this.map.containsKey(msg.customerId)) {
                        System.out.println("Customer not found.");
                        answer.success = false;
                        return answer;
                    }

                    answer = forward(msg.itemType, msg);

                    if (answer.success) {
                        Customer customer = this.map.get(msg.customerId);
                        customer.reserve(String.valueOf(msg.itemType), msg.key, answer.price);
                    }

                    return answer;

                } else {
                    return forward(msg.itemType, msg);
                }
        }
    }

    private TCPMessage forward(int index, TCPMessage msg) {
        TCPMessage incoming = null;

        try {
            outputs[index].writeObject(msg);
            incoming = (TCPMessage) inputs[index].readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            return incoming;
        }
    }

    private TCPMessage itinerary(TCPMessage msg) {

        System.out.println("Reserving an itinerary");
        TCPMessage ret = new TCPMessage();
        ret.success = false;

        if (msg.car) {
            // Reserve a car at location
            System.out.println("Reserving a car for Itinerary");
            TCPMessage carReservation = new TCPMessage();
            try {
                carReservation.id = msg.id;
                carReservation.type = 1;
                carReservation.itemType = 0;
                carReservation.actionType = 3;
                carReservation.key = msg.key;
                carReservation.customerId = msg.customerId;
                carReservation.count = 1;

                outputs[0].writeObject(carReservation);

                // Wait for answer.
                TCPMessage answer = (TCPMessage) inputs[0].readObject();
                if (answer.success) {
                    System.out.println("Car reserved for Itinerary");

                    Customer customer = this.map.get(msg.customerId);
                    customer.reserve("0", msg.key, answer.price);
                }
            } catch (IOException e) {
                System.out.println(e);
            } catch (ClassNotFoundException e) {
                System.out.println(e);
            }
        }

        if (msg.room) {
            // Reserve a room at location
            TCPMessage roomReservation = new TCPMessage();
            try {
                roomReservation.id = msg.id;
                roomReservation.type = 1;
                roomReservation.itemType = 2;
                roomReservation.actionType = 3;
                roomReservation.key = msg.key;
                roomReservation.customerId = msg.customerId;
                roomReservation.count = 1;

                outputs[2].writeObject(roomReservation);

                // Wait for answer.
                TCPMessage answer = (TCPMessage) inputs[2].readObject();
                if (answer.success) {
                    System.out.println("Room reserved for Itinerary");

                    Customer customer = this.map.get(msg.customerId);
                    customer.reserve("2", msg.key, answer.price);
                }
            } catch (IOException e) {
                System.out.println(e);
            } catch (ClassNotFoundException e) {
                System.out.println(e);
            }
        }

        Iterator i = msg.flights.iterator();
        while (i.hasNext()) {
            TCPMessage flightReservation = new TCPMessage();
            String flightNumber = (String) i.next();
            try {
                flightReservation.id = msg.id;
                flightReservation.type = 1;
                flightReservation.itemType = 1;
                flightReservation.actionType = 3;
                flightReservation.key = flightNumber;
                flightReservation.customerId = msg.customerId;

                outputs[1].writeObject(flightReservation);

                // Wait for an answer
                TCPMessage answer = (TCPMessage) inputs[1].readObject();
                ret.success = answer.success;
                if (!ret.success) {
                    return ret;
                } else {
                    System.out.println("Flight reserved for itinerary");

                    Customer customer = this.map.get(msg.customerId);
                    customer.reserve("1", flightNumber, answer.price);
                }

            } catch (IOException e) {
                System.out.println(e);
            } catch (ClassNotFoundException e) {
                System.out.println(e);
            }
        }

        return ret;
    }

    // Customer operations.
    private TCPMessage handleCustomer(TCPMessage msg) {
        TCPMessage answer = new TCPMessage();
        switch (msg.actionType){
            case 0:
                Customer cust = getCustomer(msg.customerId);
                answer.bill = cust != null ? cust.printBill() : "Customer not found";
                break;
            case 1:
                answer.customerId = addCustomer(msg.customerId);
                answer.success = (answer.customerId != -1);
                break;
            case 2:
                answer.success = removeCustomer(msg.id, msg.customerId);
                break;
            default:
                System.out.println("Unsupported customer action");
                break;
        }
        return answer;
    }

    private Customer getCustomer(int customerID) {
        return map.get(customerID);
    }

    private int addCustomer(int customerID) {
        if (customerID == 0) {
            System.out.print("Creating a new ID");
            customerID = map.size() + 5;
        }

        System.out.println("Attempting to add customer: " + customerID);
        if (map.containsKey(customerID)) {
            // Customer already present
            System.out.println("ID already in use");
            return -1;
        } else {
            Customer customer = new Customer(customerID);
            map.put(customerID, customer);
            return customerID;
        }
    }

    private boolean removeCustomer(int id, int customerID) {
        // TODO: Notify all the RMs of the removed customer.
        System.out.println("Removing customer with key: " + customerID);

        Customer customer = map.remove(customerID);

        if (customer == null) return false;

        return true;
    }
}
