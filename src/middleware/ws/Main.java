package middleware.ws;

import middleware.MiddleWareImpl;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class Main {
    public static void main(String[] args)
            throws Exception {

        if (args.length != 7) {
            System.out.println(
                    "Usage: java Main <middleware-port> <rm1-address> <rm1-port> <rm2-address> <rm2-port> <rm3-address> <rm3-port>");
            System.exit(-1);
        }

        int port = Integer.parseInt(args[0]);
        String address1 = args[1];
        int port1 = Integer.parseInt(args[2]);

        String address2 = args[3];
        int port2 = Integer.parseInt(args[4]);

        String address3 = args[5];
        int port3 = Integer.parseInt(args[6]);

        MiddleWareImpl mw = new MiddleWareImpl(port, address1, port1, address2, port2, address3, port3);

        mw.startMiddlware();
    }
}
