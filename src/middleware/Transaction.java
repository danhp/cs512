package middleware;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Transaction implements Serializable {
    private int id;

    private List<Operation> history = new ArrayList<Operation>();

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

    public boolean equals(Transaction transaction) {
        return this.id == transaction.id;
    }
}
