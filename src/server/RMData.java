package server;

import utils.Constants.TransactionStatus;

import java.io.Serializable;
import java.util.Map;

public class RMData implements Serializable {
    private RMHashtable data;
    private Map<Integer, Map<String, RMItem>> readSet;
    private Map<Integer, Map<String, RMItem>> writeSet;
    private Map<Integer, TransactionStatus> status;

    public RMData(RMHashtable data,
                  Map<Integer, Map<String, RMItem>> readSet,
                  Map<Integer, Map<String, RMItem>> writeSet,
                  Map<Integer, TransactionStatus> status){
        this.data = data;
        this.readSet = readSet;
        this.writeSet = writeSet;
        this.status = status;
    }

    public RMHashtable getData() {
        return data;
    }

    public Map<Integer, Map<String, RMItem>> getReadSet() {
        return this.readSet;
    }

    public Map<Integer, Map<String, RMItem>> getWriteSet() {
        return this.writeSet;
    }

    public Map<Integer, TransactionStatus> getStatus() {
        return status;
    }
}
