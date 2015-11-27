package server;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

class ReplicaManager {
    private Replica primary;
    private Replica local;
    private List<Replica> replicas = new ArrayList<Replica>();

    public ReplicaManager(String cluster) {
        local = new Replica(cluster);
        try {
            local.start();
        } catch (Exception e) {
            Trace.error(e.toString());
            e.printStackTrace();
        }
    }

    class Replica extends ReceiverAdapter {
        private JChannel channel;
        private String user_name = Integer.toString((int)(Math.random() * 500));
        private String cluster;

        public Replica(String cluster) {
            this.cluster = cluster;
        }

        private void start() throws Exception {
            System.out.println("Starting replica " + user_name);

            channel = new JChannel();   // use defualt config
            channel.setReceiver(this);
            channel.connect(this.cluster);
            eventLoop();
            channel.close();
        }

        private void eventLoop() {
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
                    System.out.print("> ");
                    System.out.flush();
                    String line = in.readLine().toLowerCase();
                    if (line.startsWith("quit") || line.startsWith("exit"))
                        break;
                    line = "[" + user_name + "] " + line;
                    Message msg = new Message(null, null, line);  //null: to everyone, null: own address, line: message
                    channel.send(msg);
                } catch (Exception e) {
                }
            }
        }

        // When an instance joins the cluster, or existing one leaves
        public void viewAccepted(View new_view) {
            System.out.println("** view: " + new_view);
        }

        // When message received
        public void receive(Message msg) {
            System.out.println(msg.getSrc() + ": " + msg.getObject());
        }
    }
}
