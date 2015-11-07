package middlewaremain;

import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class Main {
    public static void main(String[] args)
            throws Exception {

        if (args.length != 9) {
            System.out.println(
                    "Usage: java Main <service-name> <service-port> <server1-name> <server1-port> <server2-name> <server2-port> <server3-name> <server3-port> <deploy-dir>");
            System.exit(-1);
        }

        String serviceName = args[0];
        int servicePort = Integer.parseInt(args[1]);

        String hosts[] = {args[2], args[4], args[6] };
        int ports[] = {Integer.parseInt(args[3]), Integer.parseInt(args[5]), Integer.parseInt(args[7]) };

        String deployDir = args[8];
        Tomcat tomcat = new Tomcat();

        System.out.println("Starting server at " + tomcat.getHost().getName() + tomcat.getService().getServer().getAddress());

        tomcat.setPort(servicePort);
        tomcat.setBaseDir(deployDir);

        tomcat.getHost().setAppBase(deployDir);
        tomcat.getHost().setDeployOnStartup(true);
        tomcat.getHost().setAutoDeploy(true);

        //tomcat.addWebapp("", new File(deployDir).getAbsolutePath());

        tomcat.addWebapp("/" + serviceName,
                new File(deployDir + "/" + serviceName).getAbsolutePath());

        tomcat.start();
        tomcat.getServer().await();
    }
}
