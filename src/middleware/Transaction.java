package middleware;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Transaction implements Serializable {
    private int id;
    private List<Operation> history = new ArrayList<Operation>();
    private List<Integer> enlistedRMs = new ArrayList<Integer>();

    public Transaction(int id) {
        this.id = id;
    }

    public void addOperation(Operation op) {
        this.history.add(op);
    }

    public List<Operation> history() {
        return history;
    }

    public int getId() { return this.id; }

    public boolean enlist(int index) { return this.enlistedRMs.add(index); }
    public List<Integer> getEnlistedRMs() { return this.enlistedRMs; }

    public boolean equals(Transaction transaction) {
        return this.id == transaction.id;
    }
}
