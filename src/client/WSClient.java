package client;

import java.net.URL;
import java.net.MalformedURLException;

public class WSClient {

    client.MiddleWareImplService service;

    client.MiddleWare proxy;

    public WSClient(String serviceName, String serviceHost, int servicePort)
    throws MalformedURLException {

        URL wsdlLocation = new URL("http", serviceHost, servicePort,
                "/" + serviceName + "/service?wsdl");

        service = new client.MiddleWareImplService(wsdlLocation);

        proxy = service.getMiddleWareImplPort();
    }

}
