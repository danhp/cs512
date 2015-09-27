package client;

import server.MiddlewareImpl;
import server.ws.Middleware;

import java.net.URL;
import java.net.MalformedURLException;

public class WSClient {

    MiddlewareImpl service;

    Middleware proxy;

    public WSClient(String serviceName, String serviceHost, int servicePort)
    throws MalformedURLException {

        URL wsdlLocation = new URL("http", serviceHost, servicePort,
                "/" + serviceName + "/service?wsdl");

        service = new ResourceManagerImplService(wsdlLocation);

        proxy = service.getResourceManagerImplPort();
    }

}
