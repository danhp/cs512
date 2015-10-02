package middleware.ws;

import middleware.MiddleWareImpl;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class Main {
    public static void main(String[] args)
            throws Exception {

        if (args.length != 1) {
            System.out.println(
                    "Usage: java Main  <service-port>");
            System.exit(-1);
        }

        int port = Integer.parseInt(args[0]);

        MiddleWareImpl mw = new MiddleWareImpl(port);

        mw.run();
    }
}
