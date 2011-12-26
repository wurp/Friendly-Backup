package com.geekcommune.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;


public interface Message {

    /**
     * Get the data to be put on the wire to send this message.
     * @return
     */
    public abstract byte[] getDataToSend();

    public int getTransactionID();
    
    public RemoteNodeHandle getDestination();

    public int getNumberOfTries();

    public abstract void setNumberOfTries(int i);

    /**
     * Get the type of this message (so far backup or restore; in the future probably includes health, etc.).
     */
    public abstract String getType();
}
