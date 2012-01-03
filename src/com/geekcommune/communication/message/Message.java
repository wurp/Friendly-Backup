package com.geekcommune.communication.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;


public interface Message {

    /**
     * Write the message to the wire
     * @return
     * @throws IOException 
     * @throws FriendlyBackupException 
     */
    public abstract void write(DataOutputStream os) throws IOException, FriendlyBackupException;

    /**
     * read the message from the wire
     * @return
     * @throws IOException 
     */
    public void read(DataInputStream is) throws IOException;

    public int getTransactionID();
    
    public RemoteNodeHandle getDestination();

    public int getNumberOfTries();

    public abstract void setNumberOfTries(int i);

    /**
     * Get the type of this message (so far backup or restore; in the future probably includes health, etc.).
     */
    public abstract String getType();
    
    public abstract int getOriginNodePort();

    public abstract State getState();
    
    enum State {
        Created,
        NeedsProcessing,
        Queued,
        Processing,
        Finished,
        Error,
    }

    public abstract void setState(State state);

    public abstract boolean isComplete();
}
