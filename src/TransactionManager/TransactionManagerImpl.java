package TransactionManager;

import middleware.Operation;
import middleware.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by hmsimmonds on 15-11-07.
 */
public class TransactionManagerImpl {

//    private Map<Integer, Transaction> transactions = new HashMap<Integer, Transaction>();
//
//
//    // TRANSACTIONS
//    public void start(int id) {
//        this.transactions.put(id, new Transaction(id));
//    }
//
//    public void commit(int id) {
//        //Unlock all
//        this.lm.UnlockAll(id);
//        this.transactions.remove(id);
//    }
//
//    public void abort(int id) {
//        //Unlock all
//        this.lm.UnlockAll(id);
//        this.transactions.remove(id);
//
//        //undo the operations on customer
//        Transaction transaction = this.transactions.get(id);
//        for (Operation op : transaction.history()) {
//            this.undo(transaction.getId(), op);
//        }
//        this.transactions.remove(id);
//    }
}
