package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;

import java.util.HashMap;
import java.util.Map;

public class TransactionManager {

    // 1 min timeout
    private static int TRANSACTION_TIMEOUT = 60000;

    private MiddleWareImpl mw;

    private LockManager lockManager = new LockManager();

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

//        try {
//            // Get all the locks
//            for (Operation op: toCommit.history()) {
//
//            }
//        } catch (DeadlockException e) {
//            System.out .println("Deadlocked while committing transaction: " + id);
//            this.abort(id);
//            return false;
//        }

        // Execute all the operations
        mw.getCarProxy().commit(id);
        mw.getFlightProxy().commit(id);
        mw.getRoomProxy().commit(id);
        mw.commitCustomer(id);


        // Unlock everything
        this.lockManager.UnlockAll(id);

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

    // Essential for tracking which locks to request.
    public void addOperation(int transactionID, Operation op) {
        Transaction t = this.activeTransactions.get(transactionID);
        if (t != null) {
           t.addOperation(op);
        }

    }

    public boolean isActive() {
        return !activeTransactions.isEmpty();
    }
}
