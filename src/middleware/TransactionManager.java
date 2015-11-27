package middleware;

import server.Trace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by justindomingue on 2015-11-10.
 */
public class TransactionManager {

    // 30sec timeout
    private static final long TRANSACTION_TIMEOUT = 30000;

    private Map<Integer, List<Integer>> activeTransactions = new ConcurrentHashMap<>();
    private List<Transaction> transactions;

    private MiddleWareImpl middleware;
    private middleware.ResourceManager carProxy;
    private middleware.ResourceManager flightProxy;
    private middleware.ResourceManager roomProxy;

    private static int CAR_PROXY_INDEX = 0;
    private static int FLIGHT_PROXY_INDEX = 1;
    private static int ROOM_PROXY_INDEX = 2;

    private Map<Integer, ExpireTime> expireTimeMap = new ConcurrentHashMap<>();
    class ExpireTime {
        private long expireTime;

        public ExpireTime(long time) {
            this.expireTime = time;
        }
        public void setExpireTime(long newTime) {this.expireTime = newTime;}
        public long getExpireTime() {return this.expireTime;}
    }

    public TransactionManager(MiddleWareImpl middleware,
                                  middleware.ResourceManager carProxy,
                                  middleware.ResourceManager flightProxy,
                                  middleware.ResourceManager roomProxy ) {
        this.middleware = middleware;
        this.carProxy = carProxy;
        this.flightProxy = flightProxy;
        this.roomProxy = roomProxy;

        transactions = new ArrayList<Transaction>();

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
        //Create id
        int id;
        synchronized (transactions) {
            id = transactions.size();
            this.transactions.add(new Transaction(id));
        }

        synchronized (activeTransactions) {
            this.activeTransactions.put(id, new ArrayList<Integer>());
        }

        synchronized (expireTimeMap) {
            this.expireTimeMap.put(id, new ExpireTime(System.currentTimeMillis() + TRANSACTION_TIMEOUT));
        }

        return id;
    }

    public boolean commit(int id) {
        synchronized (activeTransactions) {
            if (!transactionExists(id)) {
                Trace.error("Can't abort transaction - doesn't exist");
                return false;
            }

            synchronized (activeTransactions) {

                //1. Phase 1
                boolean all_can_commit = true;
                for (Integer rm : activeTransactions.get(id)) {
                    boolean rm_can_commit = prepareToRM(id, rm);
                    all_can_commit = all_can_commit && rm_can_commit;
                }

                //2. Phase 2

                // (a) no failures, all yes
                if (all_can_commit) {
                    for (Integer rm : activeTransactions.get(id)) {
                        commitToRM(id, rm);
                    }
                }

                // (b) at least one failure or no
                else {
                    for (Integer rm : activeTransactions.get(id)) {
                        abortToRM(id, rm);
                    }
                }

                //remove from active
                this.activeTransactions.remove(id);
            }
        }

        return true;
    }

    public boolean abort(int id) {
        synchronized (activeTransactions) {
            if (!transactionExists(id)) {
                Trace.error("Can't abort transaction - doesn't exit");
                return false;
            }

            //undo the operations on customer
            Transaction transaction = this.transactions.get(id);
            synchronized (transaction) {
                for (Operation op : transaction.history()) {
                    middleware.undo(transaction.getId(), op);
                }
            }

            //abort to RMs
            for (Integer rm : activeTransactions.get(id)) {
                abortToRM(id, rm);
            }

            //remove from active
            this.activeTransactions.remove(id);
        }

        return true;
    }

    private boolean canCommit(int id, int rmInedx) {

    }

    private middleware.ResourceManage getProxy(int rmIndex) {
        if (rmIndex == CAR_PROXY_INDEX) {
            return carProxy;
        } else if (rmIndex == FLIGHT_PROXY_INDEX) {
            return flightProxy;
        } else if (rmIndex == ROOM_PROXY_INDEX) {
            return roomProxy;
        }
    }


    private boolean prepareToRM(int id, int rmIndex) {
        return getProxy(rmIndex).prepare(id);
    }

    private void commitToRM(int id, int rmIndex) {
        getProxy(rmIndex).doCommit(id);
    }

    private void abortToRM(int id, int rmIndex) {
        getProxy(rmIndex).doAbort(id);
    }

    private void startToRM(int id, int rmIndex) {
        getProxy(rmIndex).start(id);
    }

    public void enlist(int id, int rmIndex) {
        // Reset the time upon receiving a new operation
        this.resetTimer(id);

        //add operation to transaction with Id
        List<Integer> proxies = activeTransactions.get(id);
        synchronized (proxies) {
            if (!proxies.contains(rmIndex)) {
                proxies.add(rmIndex);
                startToRM(id, rmIndex);
            }
        }
    }

    public Transaction getTransaction(int id) {
        synchronized (transactions) {
            for (Transaction t : transactions) {
                if (t.getId() == id) {
                    return t;
                }
            }
        }

        return null;
    }

    public boolean transactionExists(int id) {
        synchronized (activeTransactions) {
            return activeTransactions.containsKey(id);
        }
    }

    public boolean isActive() {
        synchronized (this.activeTransactions) {
            return !this.activeTransactions.isEmpty();
        }
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
