package utils;

public class Constants {
    public enum TransactionStatus {
        ACTIVE, UNKNOWN, COMMITTED, ABORTED, DONE
    }

    public final static String CUSTOMER_PTR = "src/data/customer/ptr.dat";
    public final static String CUSTOMER_MASTER = "src/data/customer/master.dat";
    public final static String CUSTOMER_SLAVE = "src/data/customer/slave.dat";

    public final static String TMANAGER_FILE= "src/data/tmanager/master.dat";
}
