package middleware;

import javax.jws.WebService;
import java.net.MalformedURLException;
import java.util.Vector;
import java.net.URL;

public class MiddleWareImpl implements middleware.ws.MiddleWare {

    public MiddleWareImpl() {

    }

    public boolean addFlight(int id, int flightNumber, int numSeats, int flightPrice) {
        return false;
    }

    public boolean deleteFlight(int id, int flightNumber) {
        return false;
    }

    public int queryFlight(int id, int flightNumber) {
        return 0;
    }

    public int queryFlightPrice(int id, int flightNumber) {
        return 0;
    }

    public boolean addCars(int id, String location, int numCars, int carPrice) {
        return false;
    }

    public boolean deleteCars(int id, String location) {
        return false;
    }

    public int queryCars(int id, String location) {
        return 0;
    }

    public int queryCarsPrice(int id, String location) {
        return 0;
    }

    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
        return false;
    }

    public boolean deleteRooms(int id, String location) {
        return false;
    }

    public int queryRooms(int id, String location) {
        return 0;
    }

    public int queryRoomsPrice(int id, String location) {
        return 0;
    }

    public int newCustomer(int id) {
        return 0;
    }

    public boolean newCustomerId(int id, int customerId) {
        return false;
    }

    public boolean deleteCustomer(int id, int customerId) {
        return false;
    }

    public String queryCustomerInfo(int id, int customerId) {
        return null;
    }

    public boolean reserveFlight(int id, int customerId, int flightNumber) {
        return false;
    }

    public boolean reserveCar(int id, int customerId, String location) {
        return false;
    }

    public boolean reserveRoom(int id, int customerId, String location) {
        return false;
    }

    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers, String location, boolean car, boolean room) {
        return false;
    }
}