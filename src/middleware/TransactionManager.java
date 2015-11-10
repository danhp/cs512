package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;

public class TransactionManager {

    // 1 min timeout
    private static int TRANSACTION_TIMEOUT = 60000;

    private MiddleWareImpl mw;

    private LockManager customerLock = new LockManager();

    private Map<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
    private Map<Integer, Transaction> activeTransactions = new HashMap<Integer, Transaction>();

    public TransactionManager(MiddleWareImpl mw) {
        this.mw = mw;
    }

    public int start() {
        int id = transactions.size();

        Transaction newTransaction = new Transaction(id);

        transactions.put(id, newTransaction);
        activeTransactions.put(id, newTransaction);

        mw.getCarProxy().start(id);
        mw.getFlightProxy().start(id);
        mw.getRoomProxy().start(id);
        mw.startCustomer(id);

        return id;
    }

    public boolean commit(int id) {
        Transaction toCommit = activeTransactions.get(id);

        try {

            // Get all the locks
            for (Operation op: toCommit.history()) {

            }

            customerLock.Lock(id, "", 1);

            // Execute all the operations

            // Unlock everything
            this.customerLock.UnlockAll(id);
//            this.carLock.UnlockAll(id);
//            this.flightLock.UnlockAll(id);
//            this.roomLock.UnlockAll(id);

        } catch (DeadlockException e) {
            System.out .println("Deadlocked while committing.");
            this.abort(id);
            return false;
        }

        this.activeTransactions.remove(id);
        return true;
    }

    public boolean abort(int id) {
        mw.getCarProxy().abort(id);
        mw.getFlightProxy().abort(id);
        mw.getRoomProxy().abort(id);
        mw.abortCustomer(id);

        this.activeTransactions.remove(id);
        return true;
    }

    // Properly lock
    public void addOperation(int id, Operation op) {

    }

    public boolean isActive() {
        return activeTransactions.isEmpty();
    }
}
