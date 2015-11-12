package middleware;

import server.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by justindomingue on 2015-11-10.
 */
public class TransactionManager {

    private Map<Integer, List<Integer>> activeTransactions = new HashMap<Integer, List<Integer>>();
    private List<Transaction> transactions;

    private MiddleWareImpl middleware;
    private middleware.ResourceManager carProxy;
    private middleware.ResourceManager flightProxy;
    private middleware.ResourceManager roomProxy;

    private static int CAR_PROXY_INDEX = 0;
    private static int FLIGHT_PROXY_INDEX = 1;
    private static int ROOM_PROXY_INDEX = 2;

    public TransactionManager(MiddleWareImpl middleware,
                                  middleware.ResourceManager carProxy,
                                  middleware.ResourceManager flightProxy,
                                  middleware.ResourceManager roomProxy ) {
        this.middleware = middleware;
        this.carProxy = carProxy;
        this.flightProxy = flightProxy;
        this.roomProxy = roomProxy;

        transactions = new ArrayList<Transaction>();
    }

    public int start() {
        //Create id
        int id = transactions.size();
        this.transactions.add(new Transaction(id));
        this.activeTransactions.put(id, new ArrayList<Integer>());
        return id;
    }

    public boolean commit(int id) {
        if (!transactionExists(id)) {
            Trace.error("Can't abort transaction - doesn't exist");
            return false;
        }

        //commit to rms
        for (Integer rm : activeTransactions.get(id)) {
            commitToRM(id, rm);
        }

        //remove from active
        this.activeTransactions.remove(id);
        return true;
    }

    public boolean abort(int id) {
        if (!transactionExists(id)) {
            Trace.error("Can't abort transaction - doesn't exit");
            return false;
        }

        //undo the operations on customer
        Transaction transaction = this.transactions.get(id);
        for (Operation op : transaction.history()) {
            middleware.undo(transaction.getId(), op);
        }

        //abort to RMs
        for (Integer rm : activeTransactions.get(id)) {
            abortToRM(id, rm);
        }

        //remove from active
        this.activeTransactions.remove(id);

        return true;
    }

    private void commitToRM(int id, int rmIndex) {
        if (rmIndex == CAR_PROXY_INDEX) {
            carProxy.commit(id);
        } else if (rmIndex == FLIGHT_PROXY_INDEX) {
            flightProxy.commit(id);
        } else {
            roomProxy.commit(id);
        }
    }

    private void abortToRM(int id, int rmIndex) {
        if (rmIndex == CAR_PROXY_INDEX) {
            carProxy.abort(id);
        } else if (rmIndex == FLIGHT_PROXY_INDEX) {
            flightProxy.abort(id);
        } else {
            roomProxy.abort(id);
        }
    }

    private void startToRM(int id, int rmIndex) {
        if (rmIndex == CAR_PROXY_INDEX) {
            carProxy.start(id);
        } else if (rmIndex == FLIGHT_PROXY_INDEX) {
            flightProxy.start(id);
        } else {
            roomProxy.start(id);
        }
    }

    public void enlist(int id, int rmIndex) {
        //add operation to transaction with Id
        List<Integer> proxies = activeTransactions.get(id);
        if (!proxies.contains(rmIndex)) {
            proxies.add(rmIndex);
            startToRM(id, rmIndex);
        }
    }

    public Transaction getTransaction(int id) {
        for (Transaction t : transactions) {
            if (t.getId() == id) {
                return t;
            }
        }

        return null;
    }

    public boolean transactionExists(int id) {
        return activeTransactions.containsKey(id);
    }
}
