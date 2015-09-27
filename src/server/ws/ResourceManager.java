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
    ReservableItem getItem(int key);

//    @WebMethod
    boolean addItem(int key, ReservableItem item);

//    @WebMethod
    boolean removeItem(int key);
}