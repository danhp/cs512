package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;
import server.Trace;
import utils.Constants;
import utils.Constants.TransactionStatus;
import utils.Storage;

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

    private int transactions;
    private Map<Integer, Transaction> activeTransactions;
    private Map<Integer, Long> expireTimeMap;
    private Map<Integer, TransactionStatus> statusMap;

    public TransactionManager(MiddleWareImpl mw) {
        this.mw = mw;

        // Recover if a file is found.
        try {
            TMData data = (TMData) Storage.get(Constants.TMANAGER_FILE);
            System.out.println("Recovering from file");
            this.transactions = data.getTransactionCount();
            this.activeTransactions = data.getActiveTransactions();
            this.expireTimeMap = data.getExpireTimes();
            this.statusMap = data.getStatusMap();

        } catch(Exception e) {
            System.out.println("File either not found or corrupted\nStarting new Trans Man");
            this.transactions = 0;
            this.activeTransactions = new ConcurrentHashMap<>();
            this.expireTimeMap = new ConcurrentHashMap<>();
            this.statusMap = new ConcurrentHashMap<>();
        }

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

        this.activeTransactions.put(id, newTransaction);

        mw.getCarProxy().start(id);
        mw.getFlightProxy().start(id);
        mw.getRoomProxy().start(id);
        mw.startCustomer(id);

        this.expireTimeMap.put(id, System.currentTimeMillis() + TRANSACTION_TIMEOUT);
        this.statusMap.put(id, TransactionStatus.ACTIVE);

        this.save();

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

                shouldCrash(id, "mw", "about to send vote requests");

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

                class HaveYouCommitted implements Callable<String> {
                    private String rm;
                    private boolean decision;
                    public HaveYouCommitted(String rm, boolean decision) {
                        this.rm = rm;
                        this.decision = decision;
                    }

                    @Override
                    public String call() throws Exception {
                        try {
                            mw.haveYouCommitted(id, this.rm);
                        } catch (Exception e) {
                            Trace.error("Transaction " + id + ": RM " + this.rm + " probably died in an uncertain state. Waiting 10s econds and then sending the decision again.");
                            Thread.sleep(COMMITTED_REQUEST_TIMEOUT);

                            Trace.info("Transaction " + id + ": sending decision to RM " + this.rm);
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


                // Phase 1.
                // Check if we can still commit
                ExecutorService executor = Executors.newSingleThreadExecutor();
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
                    e.printStackTrace();
                    abort = true;
                } catch (InterruptedException e) { e.printStackTrace(); }
                  catch (ExecutionException e) {
                      Trace.error("Transaction " + id + " : One of the RMs has aborted. Aborting all.");
                      Trace.error(e.getCause().toString());
                      e.printStackTrace();
                      abort = true;
                  }

                executor.shutdown();

                shouldCrash(id, "mw", "about to send decision (decision is commit=" + !abort + ")");

                if (abort) {
                    this.abort(id);
                    return false;
                }

                Trace.info("Every RM is prepared for commit. Commiting to all.");

                // Phase 2.
                // Execute all the operations
                mw.getCarProxy().doCommit(id);
                mw.getFlightProxy().doCommit(id);
                mw.getRoomProxy().doCommit(id);
                mw.commitCustomer(id);

                shouldCrash(id, "mw", "after having sent all decisions");

                // Collecting decisions
                executor = Executors.newSingleThreadExecutor();
                future1 = executor.submit(new HaveYouCommitted("flight", !abort));
                future2 = executor.submit(new HaveYouCommitted("room", !abort));
                future3 = executor.submit(new HaveYouCommitted("car", !abort));

                try {
                    future1.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                    future2.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                    future3.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                } catch(TimeoutException e) {
                    Trace.error("Transaction " + id + " : thread timed out while requesting vote: " + e);
                    e.printStackTrace();
                } catch (InterruptedException e) { e.printStackTrace(); }
                catch (ExecutionException e) {
                    Trace.error("Transaction " + id + " : One of the RMs has aborted. Aborting all.");
                    Trace.error(e.getCause().toString());
                    e.printStackTrace();
                }

                executor.shutdown();
                // Unlock everything
                Trace.info("Unlocking every acquired locks for transaction " + id);
                this.lockManager.UnlockAll(id);

                this.activeTransactions.remove(id);
                this.expireTimeMap.remove(id);
                return true;
            }
        }
    }

    public boolean abort(int id) {
        if (!this.activeTransactions.containsKey(id)) return false;

        mw.getCarProxy().doAbort(id);
        mw.getFlightProxy().doAbort(id);
        mw.getRoomProxy().doAbort(id);
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
        this.expireTimeMap.put(transactionID, System.currentTimeMillis() + TRANSACTION_TIMEOUT);
    }

    private void cleanup() {
        synchronized (this.expireTimeMap) {
            for (Map.Entry<Integer, Long> entry : this.expireTimeMap.entrySet()) {
                if (entry.getValue() < System.currentTimeMillis()) {
                    System.out.println("Transaction " + entry.getKey() + " expired.");
                    this.abort(entry.getKey());
                }
            }
        }
    }

    private void save() {
        TMData toSave = new TMData(this.transactions,
                                   this.activeTransactions,
                                   this.expireTimeMap,
                                   this.statusMap);
        try {
            Storage.set(toSave, Constants.TMANAGER_FILE);
            System.out.println("saved");
        } catch (Exception e) {
            System.out.println(e);
            System.out.println("Failed to write to disk");
        }
    }
}
