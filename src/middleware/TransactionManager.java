package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;
import server.Trace;

import javax.print.Doc;
import java.util.Map;
import java.util.concurrent.*;
import java.util.Scanner;

public class TransactionManager {

    // 1 min timeout
    private static long TRANSACTION_TIMEOUT = 60000;
    private static long VOTE_REQUEST_TIMEOUT = 10;  //10 seconds
    private static long COMMITTED_REQUEST_TIMEOUT = 10;  //10 seconds

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

    private void shouldCrash(int id, String which, String msg) {
        Scanner scanIn = new Scanner(System.in);

        Trace.warn("Transaction " + id + ": " + msg + " - should I crash? (j/k)");
        char c = scanIn.next().charAt(0);

        if (c == 'j')
            mw.crash(which);
    }

    private boolean allShouldPrepare(int id) {
        class PrepareYourself implements Callable<String> {
            private String rm;
            public PrepareYourself(String rm) {
                this.rm = rm;
            }

            @Override
            public String call() throws Exception {
                mw.prepare(id, this.rm);
                return this.rm + " prepared!";
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);
        Future<String> future1 = executor.submit(new PrepareYourself("flight"));
        Future<String> future2 = executor.submit(new PrepareYourself("room"));
        Future<String> future3 = executor.submit(new PrepareYourself("car"));

        shouldCrash(id, "mw", "about to receive vote requests");

        boolean abort = false;
        try {
            future1.get(VOTE_REQUEST_TIMEOUT, TimeUnit.SECONDS);
            future2.get(VOTE_REQUEST_TIMEOUT, TimeUnit.SECONDS);

            shouldCrash(id, "mw", "about to receive last vote request (have received some but not all)");

            future3.get(VOTE_REQUEST_TIMEOUT, TimeUnit.SECONDS);
        } catch(TimeoutException e) {
            Trace.error("Transaction " + id + " : thread timed out while requesting vote: " + e);
            abort = true;
        } catch (InterruptedException e) { e.printStackTrace(); }
        catch (ExecutionException e) {
            Trace.error("Transaction " + id + " : ExecutionException while waiting for RM - RM has aborted.");
//            Trace.error(e.getCause().toString());
//            e.printStackTrace();
            abort = true;
        }

        executor.shutdown();

        return abort;
    }

    public boolean allDoCommitOrAbort(int id, boolean doCommit) {
        class PrayDoCommit implements Callable<String> {
            private String rm;
            private boolean decision;
            public PrayDoCommit(String rm, boolean decision) {
                this.rm = rm;
                this.decision = decision;
            }

            @Override
            public String call() throws Exception {
                Trace.info("Transaction " + id + ": sending decision commit="+doCommit+ " to RM " + this.rm);
                try {
                    if (doCommit)
                        mw.getProxy(this.rm).doCommit(id);
                    else
                        mw.getProxy(this.rm).doAbort(id);
                } catch (Exception e) {
                    Trace.error("Transaction " + id + ": RM " + this.rm + " probably died in an uncertain state. Waiting 10 seconds and then sending the decision again.");
                    Thread.sleep(COMMITTED_REQUEST_TIMEOUT);

                    if (this.decision) {
                        mw.getProxy(this.rm).doCommit(id);
                    } else {
                        mw.getProxy(this.rm).doAbort(id);
                    }
                    Thread.sleep(COMMITTED_REQUEST_TIMEOUT);

                    Trace.info("Transaction " + id + ": asking RM " + this.rm + " if it has committed");
                    mw.haveYouCommitted(id, this.rm);
                }
                return this.rm + " has committed!";
            }
        }

        // Abort CUSTOMER
        if (doCommit)
            mw.commitCustomer(id);
        else
            mw.abortCustomer(id);

        shouldCrash(id, "mw", "after having sent all decisions");

        // Collecting decisions
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future1 = executor.submit(new PrayDoCommit("flight", doCommit));
        Future<String> future2 = executor.submit(new PrayDoCommit("room", doCommit));
        Future<String> future3 = executor.submit(new PrayDoCommit("car", doCommit));

        try {
            try {
                future1.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Trace.error("Transaction " + id + " : flight RM timed out while requesting vote: " + e);

                // Resend decision
                Future<String> future = executor.submit(new PrayDoCommit("flight", doCommit));
                try {
                future.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                } catch (TimeoutException e2) {
                    Trace.error("Transaction " + id + " : RM flight didn't respond to second request.");
                }
            }
            try {
                future2.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Trace.error("Transaction " + id + " : room RM timed out while requesting vote: " + e);

                // Resend decision
                Future<String> future = executor.submit(new PrayDoCommit("room", doCommit));
                try {
                    future.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                } catch (TimeoutException e2) {
                    Trace.error("Transaction " + id + " : RM room didn't respond to second request.");
                }
            }
            try {
                future3.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                Trace.error("Transaction " + id + " : car RM timed out while requesting vote: " + e);

                // Resend decision
                Future<String> future = executor.submit(new PrayDoCommit("car", doCommit));
                try {
                    future.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                } catch (TimeoutException e2) {
                    Trace.error("Transaction " + id + " : RM car didn't respond to second request.");
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            Trace.error("Transaction " + id + " : One of the RMs has aborted. Aborting all.");
            Trace.error(e.getCause().toString());
            e.printStackTrace();
        }

        executor.shutdown();

        return true;
    }

    public boolean commit(int id) {
        if (!this.activeTransactions.containsKey(id)) return false;

        synchronized (activeTransactions) {
            Transaction toCommit = activeTransactions.get(id);

            synchronized (lockManager) {
                try {
                    // Get all the locks
                    for (Operation op : toCommit.history()) {
                        String objectID = op.getItem() + "-" + op.getKey();
                        if (op.getType() == 2) {
                            this.lockManager.Lock(id, objectID, LockManager.WRITE);
                        }
                    }
                } catch (DeadlockException e) {
                    System.out.println("Deadlocked while committing transaction: " + id);
                    this.abort(id);
                    return false;
                }
            }

            shouldCrash(id, "mw", "about to send vote requests");

            // Phase 1.
            // Check if we can still commit
            boolean vetoAbort = allShouldPrepare(id);

            shouldCrash(id, "mw", "about to send decision (decision is commit=" + !vetoAbort + ")");

            // Phase 2.
            // Execute all the operations
            if (!vetoAbort) {
                Trace.info("every rm is prepared for commit. committing to all.");
                allDoCommitOrAbort(id, true);
            } else {
                Trace.info("At least one RM has voted for abort. Aborting to all.");
                this.abort(id);
            }

            // Unlock everything
            Trace.info("Unlocking every acquired locks for transaction " + id);
            this.lockManager.UnlockAll(id);

            this.activeTransactions.remove(id);
            this.expireTimeMap.remove(id);
            return true;
        }
    }

    public boolean abort(int id) {
        if (!this.activeTransactions.containsKey(id)) return false;

        Trace.info("Aborting transaction " + id);
        allDoCommitOrAbort(id, false);  // send abort to all

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
