package middleware;

import LockManager.LockManager;

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

    private LockManager lm = new LockManager();

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

    public void start(int id) {
        this.transactions.add(new Transaction(id));
        this.activeTransactions.put(id, new ArrayList<Integer>());
    }

    public void commit(int id) {
        //Unlock all
        this.lm.UnlockAll(id);
        //remove and place into active transactions
        this.transactions.remove(id);

        for (Integer rm : activeTransactions.get(id)) {
            commitToRM(id, rm);
        }
    }

    public void abort(int id) {
        //Unlock all
        this.lm.UnlockAll(id);

        //undo the operations on customer
        Transaction transaction = this.transactions.get(id);
        for (Operation op : transaction.history()) {
            middleware.undo(transaction.getId(), op);
        }
        this.transactions.remove(id);

        for (Integer rm : activeTransactions.get(id)) {
            abortToRM(id, rm);
        }
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

    public void enlist(int id, int rmIndex) {
        //add operation to transaction with Id
        List<Integer> proxies = activeTransactions.get(id);
        if (!proxies.contains(rmIndex)) {
            proxies.add(rmIndex);
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
}
