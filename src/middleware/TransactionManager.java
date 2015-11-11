package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {

    // 1 min timeout
    private static long TRANSACTION_TIMEOUT = 30000;

    private MiddleWareImpl mw;

    private LockManager lockManager = new LockManager();

    private int transactions = 0;
    private Map<Integer, Transaction> activeTransactions = new ConcurrentHashMap<>();

    private Map<Integer, ExpireTime> expireTimeMap = new ConcurrentHashMap<>();
    class ExpireTime {
        private long expireTime;

        public ExpireTime(long time) {
            this.expireTime = time;
        }
        public void setExpireTime(long newTime) {this.expireTime = newTime;}
        public long getExpireTime() {return this.expireTime;}
    }

    public TransactionManager(MiddleWareImpl mw) {
        this.mw = mw;

        // Periodic cleanup.
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(3*TRANSACTION_TIMEOUT);
                        cleanup();
                    } catch (InterruptedException e) {
                        System.out.println("EXCEPTION: ");
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public int start() {
        int id = ++transactions;

        Transaction newTransaction = new Transaction(id);

        synchronized (activeTransactions) {
            this.activeTransactions.put(id, newTransaction);
        }

        mw.getCarProxy().start(id);
        mw.getFlightProxy().start(id);
        mw.getRoomProxy().start(id);
        mw.startCustomer(id);

        synchronized (expireTimeMap) {
            this.expireTimeMap.put(id, new ExpireTime(System.currentTimeMillis() + TRANSACTION_TIMEOUT));
        }

        System.out.println("Started transaction with new ID: " + id);
        return id;
    }

    public boolean commit(int id) {
        if (!this.activeTransactions.containsKey(id)) return false;

        synchronized (activeTransactions) {
            Transaction toCommit = activeTransactions.get(id);

            synchronized (lockManager) {
                try {
                    // Get all the locks
                    for (Operation op: toCommit.history()) {
                        String objectID = op.getItem() + "-" + op.getKey();
                        if (op.getType() == 2) {
                            this.lockManager.Lock(id, objectID, LockManager.WRITE);
                        }
                    }
                } catch (DeadlockException e) {
                    System.out .println("Deadlocked while committing transaction: " + id);
                    this.abort(id);
                    return false;
                }

                // Execute all the operations
                mw.getCarProxy().commit(id);
                mw.getFlightProxy().commit(id);
                mw.getRoomProxy().commit(id);
                mw.commitCustomer(id);

                // Unlock everything
                this.lockManager.UnlockAll(id);

                this.activeTransactions.remove(id);
                this.expireTimeMap.remove(id);
                return true;
            }
        }
    }

    public boolean abort(int id) {
        if (!this.activeTransactions.containsKey(id)) return false;

        mw.getCarProxy().abort(id);
        mw.getFlightProxy().abort(id);
        mw.getRoomProxy().abort(id);
        mw.abortCustomer(id);

        synchronized (activeTransactions) {
            this.activeTransactions.remove(id);
        }
        synchronized (expireTimeMap) {
            this.expireTimeMap.remove(id);
        }

        System.out.println("Aborted transaction: " + id);
        return true;
    }

    // Essential for tracking which locks to request.
    public boolean addOperation(int transactionID, Operation op) {
        if (!this.activeTransactions.containsKey(transactionID)) return false;

        this.resetTimer(transactionID);

        synchronized (activeTransactions) {
            Transaction t = this.activeTransactions.get(transactionID);
            if (t != null) {
                t.addOperation(op);
            }
        }

        return true;
    }

    public boolean isActive() {
        return !activeTransactions.isEmpty();
    }

    public void resetTimer(int transactionID) {
        synchronized (this.expireTimeMap) {
            long newExpireTime = System.currentTimeMillis() + TRANSACTION_TIMEOUT;
            this.expireTimeMap.get(transactionID).setExpireTime(newExpireTime);
        }
    }

    private void cleanup() {
        synchronized (this.expireTimeMap) {
            for (Map.Entry<Integer, ExpireTime> entry : this.expireTimeMap.entrySet()) {
                if (entry.getValue().getExpireTime() < System.currentTimeMillis()) {
                    System.out.println("Transaction " + entry.getKey() + " expired.");
                    this.abort(entry.getKey());
                }
            }
        }
    }
}
