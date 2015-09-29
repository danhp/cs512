package server.ws;

import server.*;

import javax.jws.WebService;
import javax.jws.WebMethod;

/**
 * Created by justindomingue on 15-09-27.
 */

//@WebService
public interface ResourceManager {

//    @WebMethod
    ReservableItem getItem(int id, String key);

//    @WebMethod
    boolean addItem(int id, ReservableItem item);

//    @WebMethod
    boolean removeItem(int id, String key);
}