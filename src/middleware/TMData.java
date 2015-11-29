package middleware;

import utils.Constants.TransactionStatus;

import java.io.Serializable;
import java.util.Map;

public class TMData implements Serializable{
    private int transactionCount;
    private Map<Integer, Transaction> activeTransactions;
    private Map<Integer, Long> expireTimes;
    private Map<Integer, TransactionStatus> statusMap;

    public TMData(int transactionCount,
                  Map<Integer, Transaction> activeTransactions,
                  Map<Integer, Long> expireTimes,
                  Map<Integer, TransactionStatus> statusMap) {
        this.transactionCount = transactionCount;
        this.activeTransactions = activeTransactions;
        this.expireTimes = expireTimes;
        this.statusMap = statusMap;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public Map<Integer, Transaction> getActiveTransactions() {
        return this.activeTransactions;
    }

    public Map<Integer, Long> getExpireTimes() {
        return this.expireTimes;
    }

    public Map<Integer, TransactionStatus> getStatusMap() {
        return this.statusMap;
    }
}
