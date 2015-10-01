package client;

import java.io.Serializable;

/**
 * Created by justindomingue on 15-10-01.
 */
public class TCPMessage implements Serializable {
    public int id;
    public int type; //0=ack,1=msg
    public int itemType;   //0=car 1=flight 2=room 3=itinerary
    public int actionType; //0=get 1=add 2=delete 3=reserve

    // specific to reservable item
    public String key;
    public int count;
    public int count2;
    public int price;

    public boolean car;
    public boolean room;

    // customers
    public int customerId;

    // success
    public boolean success;
}
