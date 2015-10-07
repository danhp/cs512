package middleware;

import server.*;

import javax.jws.WebService;
import java.net.MalformedURLException;
import java.util.Enumeration;
import java.util.List;
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
        String hosts[] = {"142.157.165.20","142.157.165.20","142.157.165.113","142.157.165.113" };
        int[] ports = {4000,4001,4002,4003};

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

                System.out.println("Connection established with " + hosts[i] + ":" + ports[i]);
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
        List<Object> reservedItems = getCustomerProxy().getCustomerReservations(id, customerId);

        if (reservedItems== null) { return false; }

        // Increase the reserved numbers of all reservable items that
        // the customer reserved.
        for (int i = 0; i < reservedItems.size()/2; i++) {
            String key = (String) reservedItems.get(i);
            int count = (int) reservedItems.get(i + reservedItems.size()/2);

            Trace.info("RM::deleteCustomer(" + id + ", " + customerId + "): "
                    + "deleting " + count + " reservations "
                    + "for item " + key);

            char fst = key.charAt(0);
            middleware.ResourceManager proxy = fst == 'c' ? getCarProxy() : fst == 'f' ? getFlightProxy() : getRoomProxy();
            proxy.unreserveItem(id, key, "", count);
        }

        return getCustomerProxy().deleteCustomer(id, customerId);
    }

    @Override
    public String queryCustomerInfo(int id, int customerId) {
        return getCustomerProxy().queryCustomerInfo(id, customerId);
    }

    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        if (!getCustomerProxy().customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Flight.getKey(flightNumber) + ", " + flightNumber + ") failed: customer doesn't exist.");
            return false;
        }

        boolean reserved = getFlightProxy().reserveFlight(id, customerId, flightNumber);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Flight.getKey(flightNumber) + ", " + flightNumber + ") failed: flight cannot be reserved.");
            return false;
        }

        int price = queryFlightPrice(id, flightNumber);
        getCustomerProxy().setCustomerReservation(id, customerId, Flight.getKey(flightNumber), String.valueOf(flightNumber), price);

        return true;
    }

    @Override
    public boolean reserveCar(int id, int customerId, String location) {
        if (!getCustomerProxy().customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Car.getKey(location) + ", " + location + ") failed: customer doesn't exist.");
            return false;
        }

        boolean reserved = getCarProxy().reserveCar(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Car.getKey(location) + ", " + location + ") failed: flight cannot be reserved.");
            return false;
        }

        int price = queryCarsPrice(id, location);
        getCustomerProxy().setCustomerReservation(id, customerId, Car.getKey(location), location, price);

        return true;
    }

    @Override
    public boolean reserveRoom(int id, int customerId, String location) {
        if (!getCustomerProxy().customerExists(id, customerId)) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Room.getKey(location) + ", " + location + ") failed: customer doesn't exist.");
            return false;
        }

        boolean reserved = getRoomProxy().reserveRoom(id, customerId, location);

        if (!reserved) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", "
                    + Room.getKey(location) + ", " + location + ") failed: flight cannot be reserved.");
            return false;
        }

        int price = queryRoomsPrice(id, location);
        getCustomerProxy().setCustomerReservation(id, customerId, Room.getKey(location), location, price);

        return true;
    }

    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
        // Assuming everything has to work for reserve itinerary to return true
        boolean result = false;

        for (Enumeration<String> e = flightNumbers.elements(); e.hasMoreElements();)
        {
            result = reserveFlight(id, customerId, Integer.parseInt(e.nextElement()));
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