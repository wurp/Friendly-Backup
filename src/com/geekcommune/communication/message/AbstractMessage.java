package com.geekcommune.communication.message;

public class AbstractMessage implements Message {
    protected static int nextTransactionId = (int) (Math.random() * Integer.MAX_VALUE);
    
    private int transactionId;

    public AbstractMessage() {
        transactionId = nextTransactionID();
    }
    
    protected static synchronized int nextTransactionID() {
        return nextTransactionId++;
    }

    public int getTransactionID() {
        return transactionId;
    }
}
