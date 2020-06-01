package org.srg.smartclient.isomorphic;

import java.util.Iterator;
import java.util.List;

public class DSTransaction implements IDSRequest {
    private int transactionNum;
    private List<DSRequest> operations;

    public List<DSRequest> getOperations() {
        return operations;
    }

    public int getTransactionNum() {
        return transactionNum;
    }

    @Override
    public Iterator<DSRequest> iterator() {
        return operations.iterator();
    }
}
