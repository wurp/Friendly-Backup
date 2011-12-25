package com.geekcommune.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;

public abstract class AbstractMessage implements Message {
    protected static int nextTransactionId = (int) (Math.random() * Integer.MAX_VALUE);
    
    private int transactionId;
    private RemoteNodeHandle destination;
    private int numTries = 0;

    public AbstractMessage(RemoteNodeHandle storingNode) {
        transactionId = nextTransactionID();
        this.destination = storingNode;
    }
    
    protected static synchronized int nextTransactionID() {
        return nextTransactionId++;
    }

    public int getTransactionID() {
        return transactionId;
    }

    public RemoteNodeHandle getDestination() {
        return destination;
    }

    public void setNumberOfTries(int i) {
        this.numTries = i;
    }

    public int getNumberOfTries() {
        return numTries;
    }
}
