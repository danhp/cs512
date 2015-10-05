package middleware;

import middleware.ws.MiddleWare;

import javax.jws.WebService;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import java.net.URL;

@WebService(endpointInterface = "middleware.ws.MiddleWare")
public class MiddleWareImpl implements middleware.ws.MiddleWare {

    middleware.ResourceManagerImplService rm;
    middleware.ResourceManager[] proxy;

    private static int CAR_PROXY_INDEX = 0;
    private static int FLIGHT_PROXY_INDEX = 1;
    private static int ROOM_PROXY_INDEX = 2;
    private static int CUSTOMER_PROXY_INDEX = 3;

    public MiddleWareImpl() {
        String hosts[] = {"localhost","localhost","localhost","localhost" };
        int[] ports = {6000,6001,6002,6003};

        setupProxies(hosts, ports);
    }

    public MiddleWareImpl(String[] hosts, int[] ports) {
        if (ports.length != hosts.length) {
            System.out.println("Ports array length doesn't match hosts array length");
            return;
        }

        setupProxies(hosts, ports);
    }

    public void setupProxies(String[] hosts, int[] ports)
    {
        proxy = new middleware.ResourceManager[ports.length];

        try {
            for (int i = 0; i < ports.length; i++) {
                URL wsdlLocation = new URL("http://" + hosts[i] + ":" + ports[i] + "/rm/service?wsdl");
                rm = new middleware.ResourceManagerImplService(wsdlLocation);
                proxy[i] = rm.getResourceManagerImplPort();
            }
        } catch (MalformedURLException e) {
            System.out.println(e);
        }
    }


    private middleware.ResourceManager getCarProxy() { return proxy[MiddleWareImpl.CAR_PROXY_INDEX]; }
    private middleware.ResourceManager getFlightProxy() { return proxy[MiddleWareImpl.FLIGHT_PROXY_INDEX]; }
    private middleware.ResourceManager getRoomProxy() { return proxy[MiddleWareImpl.ROOM_PROXY_INDEX]; }
    private middleware.ResourceManager getCustomerProxy() { return proxy[MiddleWareImpl.CUSTOMER_PROXY_INDEX]; }

    @Override
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        return getFlightProxy().addFlight(id, flightNumber, numSeats, flightPrice);
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        return getFlightProxy().deleteFlight(id, flightNumber);
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        return getFlightProxy().queryFlight(id, flightNumber);
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        return getFlightProxy().queryFlightPrice(id, flightNumber);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        return getCarProxy().addCars(id, location, numCars, carPrice);
    }

    @Override
    public boolean deleteCars(int id, String location) {
        return getCarProxy().deleteCars(id, location);
    }

    @Override
    public int queryCars(int id, String location) {
        return getCarProxy().queryCars(id, location);
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        return getCarProxy().queryCarsPrice(id, location);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        return getRoomProxy().addRooms(id, location, numRooms, roomPrice);
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        return getRoomProxy().deleteRooms(id, location);
    }

    @Override
    public int queryRooms(int id, String location) {
        return getRoomProxy().queryRooms(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
        return getRoomProxy().queryRoomsPrice(id, location);
    }

    @Override
    public int newCustomer(int id) {
        return getCustomerProxy().newCustomer(id);
    }

    @Override
    public boolean newCustomerId(int id, int customerId) {
        return getCustomerProxy().newCustomerId(id, customerId);
    }

    @Override
    public boolean deleteCustomer(int id, int customerId) {
        return getCustomerProxy().deleteCustomer(id, customerId);
    }

    @Override
    public String queryCustomerInfo(int id, int customerId) {
        return getCustomerProxy().queryCustomerInfo(id, customerId);
    }

    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        return getFlightProxy().reserveFlight(id, customerId, flightNumber);
    }

    @Override
    public boolean reserveCar(int id, int customerId, String location) {
        return getCarProxy().reserveCar(id, customerId, location);
    }

    @Override
    public boolean reserveRoom(int id, int customerId, String location) {
        return getRoomProxy().reserveRoom(id, customerId, location);
    }

    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
        // Assuming everything has to work for reserve itinerary to return true
        boolean result = false;

        for (Enumeration<Integer> e = flightNumbers.elements(); e.hasMoreElements();)
        {
            result = reserveFlight(id, customerId, e.nextElement());
        }

        if (car) {
            result = reserveCar(id, customerId, location);
        }

        if (room) {
            result = reserveRoom(id, customerId, location);
        }

        return result;
    }
}