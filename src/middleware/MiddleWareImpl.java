package middleware;

import javax.jws.WebService;
import java.net.MalformedURLException;
import java.util.Vector;
import java.net.URL;

@WebService(endpointInterface = "middleware.ws.MiddleWare")
public class MiddleWareImpl implements middleware.ws.MiddleWare {

    middleware.ResourceManagerImplService rm;
    middleware.ResourceManager proxy;

    public MiddleWareImpl() throws MalformedURLException {
        URL wsdlLocation = new URL("http://localhost:8080/rm/service?wsdl");

        rm = new middleware.ResourceManagerImplService(wsdlLocation);
        proxy = rm.getResourceManagerImplPort();
    }

    @Override
    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        return proxy.addFlight(id, flightNumber, numSeats, flightPrice);
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
        return proxy.deleteFlight(id, flightNumber);
    }

    @Override
    public int queryFlight(int id, int flightNumber) {
        return queryFlight(id, flightNumber);
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) {
        return queryFlightPrice(id, flightNumber);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
        return addCars(id, location, numCars, carPrice);
    }

    @Override
    public boolean deleteCars(int id, String location) {
        return proxy.deleteCars(id, location);
    }

    @Override
    public int queryCars(int id, String location) {
        return proxy.queryCars(id, location);
    }

    @Override
    public int queryCarsPrice(int id, String location) {
        return proxy.queryCarsPrice(id, location);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        return proxy.addRooms(id, location, numRooms, roomPrice);
    }

    @Override
    public boolean deleteRooms(int id, String location) {
        return proxy.deleteRooms(id, location);
    }

    @Override
    public int queryRooms(int id, String location) {
        return proxy.queryRooms(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) {
        return proxy.queryRoomsPrice(id, location);
    }

    @Override
    public int newCustomer(int id) {
        return proxy.newCustomer(id);
    }

    @Override
    public boolean newCustomerId(int id, int customerId) {
        return proxy.newCustomerId(id, customerId);
    }

    @Override
    public boolean deleteCustomer(int id, int customerId) {
        return proxy.deleteCustomer(id, customerId);
    }

    @Override
    public String queryCustomerInfo(int id, int customerId) {
        return proxy.queryCustomerInfo(id, customerId);
    }

    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        return proxy.reserveFlight(id, customerId, flightNumber);
    }

    @Override
    public boolean reserveCar(int id, int customerId, String location) {
        return proxy.reserveCar(id, customerId, location);
    }

    @Override
    public boolean reserveRoom(int id, int customerId, String location) {
        return proxy.reserveRoom(id, customerId, location);
    }

    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
        return proxy.reserveItinerary(id, customerId, flightNumbers, location, car, room);
    }
}