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
    private ObjectInputStream[] inputs = new ObjectInputStream[3];
    private ObjectOutputStream[] outputs = new ObjectOutputStream[3];

    public MiddleWareImpl(int port,
                          String carHost, int carPort,
                          String flightHost, int flightPort,
                          String roomHost, int roomPort) {
        try {
            System.out.println("Creating middleware at " + port);
            serverSocket = new ServerSocket(port);

            System.out.println("Connecting to the resource manager ");
            rmSockets[0] = new Socket(carHost, carPort);
            outputs[0] = new ObjectOutputStream(rmSockets[0].getOutputStream());
            inputs[0] = new ObjectInputStream(rmSockets[0].getInputStream());

        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try {
            rmSockets[1] = new Socket(flightHost, flightPort);
            outputs[1] = new ObjectOutputStream(rmSockets[1].getOutputStream());
            inputs[1] = new ObjectInputStream(rmSockets[1].getInputStream());
        } catch (IOException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        try{
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

    private TCPMessage decode(TCPMessage msg) {
        Map<String, Object> map = new HashMap<>();

        System.out.println("Type" + msg.type + " itemtype " + msg.itemType + " actiontype: + " + msg.actionType);
        if (msg.type == 0) return msg;

        return forward(msg.itemType, msg);
    }
}