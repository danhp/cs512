package server.ws;

import java.io.File;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import server.ResourceManagerImpl;


public class Main {

    public static void main(String[] args) 
    throws Exception {

        if (args.length != 1) {
            System.out.println(
                    "Usage: java Main <service-port>");
            System.exit(-1);
        }

        int port = Integer.parseInt(args[0]);

        ResourceManagerImpl rm = new ResourceManagerImpl(port);
    }
    
}
