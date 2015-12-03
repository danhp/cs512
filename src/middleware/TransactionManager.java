package middleware;

import LockManager.LockManager;
import LockManager.DeadlockException;
import server.Trace;
import utils.Constants;
import utils.Constants.TransactionStatus;
import utils.Storage;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

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
            System.out.println("Recovering TM from file");
            this.transactions = data.getTransactionCount();
            this.activeTransactions = data.getActiveTransactions();
            this.expireTimeMap = data.getExpireTimes();
            this.statusMap = data.getStatusMap();

            this.recover();

        } catch(ClassNotFoundException | IOException e) {
            e.printStackTrace();
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

    private void recover() {
        Map<Integer, TransactionStatus> copy = new HashMap<>(this.statusMap);

        Trace.info("Recovering following transactions: " + copy);

        for (Map.Entry<Integer, TransactionStatus> entry : copy.entrySet()) {
            // No decision reached before the crash or decided to abort.
            if (entry.getValue() == TransactionStatus.ACTIVE || entry.getValue() == TransactionStatus.ABORTED) {
                // send abort to all
                System.out.println("Resuming with abort of: " + entry.getKey());
                this.allDoCommitOrAbort(entry.getKey(), false, new ArrayList<>(activeTransactions.get(entry.getKey()).getEnlistedRMs()));  //TODO save enlisted RMs
                this.statusMap.put(entry.getKey(), TransactionStatus.DONE);
                continue;
            }

            // Decision of committing was reached before crash
            if (entry.getValue() == TransactionStatus.COMMITTED) {
                System.out.println("Resuming commit of: " + entry.getKey());
                this.allDoCommitOrAbort(entry.getKey(), true, new ArrayList<>(activeTransactions.get(entry.getKey()).getEnlistedRMs()));   // TODO save enlisted RMs
                this.statusMap.put(entry.getKey(), TransactionStatus.DONE);
                continue;
            }

            // Ignore all other cases as no action required?
        }

    }

    public int start() {
        int id = ++transactions;

        Transaction newTransaction = new Transaction(id);

        this.activeTransactions.put(id, newTransaction);

        // start at rms will be triggered later when rm is requested
//        mw.getCarProxy().start(id);
//        mw.getFlightProxy().start(id);
//        mw.getRoomProxy().start(id);
//        mw.startCustomer(id);

        this.expireTimeMap.put(id, System.currentTimeMillis() + TRANSACTION_TIMEOUT);
        this.statusMap.put(id, TransactionStatus.ACTIVE);

//        this.save();

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

    // Ask every RM to prepare - returns a list of RMs that aborted, or empty list
    private List<Integer> allShouldPrepare(final int id, List<Integer> rms) {
        class PrepareYourself implements Callable<String> {
            private int rm;
            public PrepareYourself(int rm) {
                this.rm = rm;
            }

            @Override
            public String call() throws Exception {
                mw.prepare(id, this.rm);
                return this.rm + " prepared!";
            }
        }

        List<Integer> abortedRMs = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(rms.size());
        List<Future<String>> futures = new ArrayList<>();
        for (int rm : rms) {
            futures.add(executor.submit(new PrepareYourself(rm)));
        }

        shouldCrash(id, "mw", "about to receive vote requests");

        // Collect the answers
        for (int i = 0; i < futures.size(); i++) {
            Future<String> future = futures.get(i);
            int rm = rms.get(i);
            try {
                future.get(VOTE_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                this.statusMap.put(id, TransactionStatus.COMMITTED);
                this.save();
            } catch (TimeoutException e) {
                Trace.error("Transaction " + id + " : RM " + rm + " timed out while requesting vote.");
                abortedRMs.add(rm);
            } catch (ExecutionException e) {
                Trace.error("Transaction " + id + " : ExecutionException while waiting for RM " + rm + " - RM has aborted.");
                abortedRMs.add(rm);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();

        return abortedRMs;
    }

    public boolean allDoCommitOrAbort(final int id, final boolean doCommit, List<Integer> rms) {
        class PrayDoCommit implements Callable<String> {
            private int rm;
            private boolean wait = false;

            public PrayDoCommit(int rm) {
                this.rm = rm;
            }

            public PrayDoCommit(int rm, boolean wait) {
                this.rm = rm;
                this.wait = wait;
            }

            @Override
            public String call() throws Exception {
                if (wait) {
                    long waittime = COMMITTED_REQUEST_TIMEOUT/2;
                    Trace.info("Waiting " + waittime + "s before sending decision again.");
                    Thread.sleep(waittime*1000);
                }

                Trace.info("Transaction " + id + ": sending decision commit="+doCommit+ " to RM " + this.rm);

                if (doCommit)
                    mw.getProxy(this.rm).doCommit(id);
                else
                    mw.getProxy(this.rm).doAbort(id);

                return this.rm + " has committed!";
            }
        }

        // Abort CUSTOMER
        if (rms.contains("customer")) {
            if (doCommit)
                mw.commitCustomer(id);
            else
                mw.abortCustomer(id);
        }

        // Collecting decisions
        if (rms.size() > 0) {
            ExecutorService executor = Executors.newFixedThreadPool(rms.size());

            List<Future<String>> futures = new ArrayList<>();
            for (int rm : rms) {
                futures.add(executor.submit(new PrayDoCommit(rm)));
            }

            shouldCrash(id, "mw", "after having sent all decisions");

            for (int i = 0; i < futures.size(); i++) {
                Future<String> future = futures.get(i);
                int rm = rms.get(i);
                try {
                    future.get(COMMITTED_REQUEST_TIMEOUT, TimeUnit.SECONDS);
                    //TODO log "FLIGHT COMMITTED"
                } catch (Exception e) {
                    Trace.error("Transaction " + id + " : " + rm + " RM timed out while requesting vote.");

                    // Resend decision
                    Future<String> future2 = executor.submit(new PrayDoCommit(rm, true));
                    try {
                        future2.get(COMMITTED_REQUEST_TIMEOUT*2, TimeUnit.SECONDS);
                        break;
                    } catch (Exception e2) {
                        Trace.error("Transaction " + id + " : RM " + rm + " didn't respond to request.");
                    }
                }
            }

            executor.shutdown();
        }

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

            Set<Integer> enlistedRMs = toCommit.getEnlistedRMs();
            Trace.info("Transaction " + id + " : Starting 2PC with participants " + enlistedRMs);

            // Phase 1.
            // Check if we can still commit
            this.statusMap.put(id, TransactionStatus.ACTIVE);

            List<Integer> abortedRMs = allShouldPrepare(id, new ArrayList<>(enlistedRMs));
            boolean allPreparedToCommit = abortedRMs.isEmpty();

            shouldCrash(id, "mw", "about to send decision (decision is commit=" + allPreparedToCommit + ")");

            // Phase 2.
            // Execute all the operations
            if (allPreparedToCommit) {
                Trace.info("every rm is prepared for commit. committing to all.");
                this.statusMap.put(id, TransactionStatus.COMMITTED);
                allDoCommitOrAbort(id, true, new ArrayList<>(enlistedRMs));
            } else {
                Trace.info("At least one RM has voted for abort. Aborting to all.");
                this.statusMap.put(id, TransactionStatus.ABORTED);
                this.abort(id, abortedRMs);
            }

            // Unlock everything
            Trace.info("Unlocking every acquired locks for transaction " + id);
            this.lockManager.UnlockAll(id);

            this.activeTransactions.remove(id);
            this.expireTimeMap.remove(id);

            this.statusMap.put(id, TransactionStatus.DONE);
            this.save();

            return true;
        }
    }

    public boolean abort(int id, List<Integer> toIgnore) {
        if (!this.activeTransactions.containsKey(id)) return false;

        Trace.info("Aborting transaction " + id);

        synchronized (activeTransactions) {
            Transaction t = this.activeTransactions.get(id);
            List<Integer> toContact = new ArrayList<>(t.getEnlistedRMs());
            toContact.removeAll(toIgnore);

            allDoCommitOrAbort(id, false, toContact);  // send abort to all
            this.activeTransactions.remove(id);
        }
        synchronized (expireTimeMap) {
            this.expireTimeMap.remove(id);
        }

        System.out.println("Aborted transaction: " + id);
        this.save();

        return true;
    }

    public boolean abort(int id) {
        return abort(id, new ArrayList<Integer>());
    }

    private void enlist(int transactionID, int rm) {
        Trace.info("Enlisting RM " + rm);
        if (rm == mw.CUSTOMER_INDEX) {
            mw.startCustomer(transactionID);
        } else {
            mw.getProxy(rm).start(transactionID);
        }
    }

    // Essential for tracking which locks to request.
    public boolean addOperation(int transactionID, Operation op) {
        if (!this.activeTransactions.containsKey(transactionID)) return false;

        this.resetTimer(transactionID);

        synchronized (activeTransactions) {
            Transaction t = this.activeTransactions.get(transactionID);
            if (t != null) {
                t.addOperation(op);

                // if first time operation involves rm, start the transaction
                int rm = op.getItem();
                if (t.enlist(rm)) {
                    enlist(transactionID, rm);
                }
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
            System.out.println("Failed to write to: " + Constants.TMANAGER_FILE);
        }
    }
}
