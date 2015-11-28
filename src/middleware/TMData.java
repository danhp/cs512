package middleware;

import java.io.Serializable;
import java.util.Map;

public class TMData implements Serializable{
    private int transactionCount;
    private Map<Integer, Transaction> activeTransactions;
    private Map<Integer, Long> expireTimes;

    public TMData(int transactionCount,
                  Map<Integer, Transaction> activeTransactions,
                  Map<Integer, Long> expireTimes) {
        this.transactionCount = transactionCount;
        this.activeTransactions = activeTransactions;
        this.expireTimes = expireTimes;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public Map<Integer, Transaction> getActiveTransactions() {
        return this.activeTransactions;
    }

    public Map<Integer, Long> getExpireTiemes() {
        return this.expireTimes;
    }
}
