package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;

import java.util.HashMap;
import java.util.Map;

public class TransactionManager {

    // Centralized lock manager.
    private LockManager lm = new LockManager();

    private Map<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
    private Map<Integer, Transaction> activeTransactions = new HashMap<Integer, Transaction>();

    public void start(int id) {
        Transaction newTransaction = new Transaction(id);

        transactions.put(id, newTransaction);
        activeTransactions.put(id, newTransaction);
    }

    public void commit(int id) {
        // Get all the locks
        Transaction toCommit = activeTransactions.get(id);

        try {

            for (Operation op: toCommit.history()) {

            }

            lm.Lock(id, "", 1);

        } catch (DeadlockException e) {
            System.out .println("Deadlocked while committing.");
            this.abort(id);
        }

        this.activeTransactions.remove(id);
    }

    public void abort(int id) {
        // TODO: Tell to clear the write sets.

        this.activeTransactions.remove(id);
    }

    // Properly lock
    public void addOperation(int id, Operation op) {

    }

    public boolean isActive() {
        return activeTransactions.isEmpty();
    }
}
