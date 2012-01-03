package com.geekcommune.communication.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;

public abstract class AbstractMessage implements Message {
    public static final Object STATE_CHANGE_NOTIFIER = new Object();

    protected static int nextTransactionId = (int) (Math.random() * Integer.MAX_VALUE);

    private static Map<Integer,MessageFactory> messageFactories = new HashMap<Integer, MessageFactory>();
    
    private int transactionId;
    private RemoteNodeHandle destination;
    private int numTries = 0;
    private int originNodePort;

    private State state;

    private List<StateListener> stateListeners = new ArrayList<StateListener>();

    public AbstractMessage(RemoteNodeHandle destination, int originNodePort) {
        this(destination, nextTransactionID(), originNodePort);
    }

    /**
     * Only used when streaming off the wire
     * @param transactionId
     */
    protected AbstractMessage(int transactionId, int originNodePort) {
        this(null, transactionId, originNodePort);
    }

    /**
     * Only used when streaming off the wire
     * @param transactionId
     */
    protected AbstractMessage(int transactionId, RemoteNodeHandle destination, int originNodePort) {
        this(null, transactionId, originNodePort);
        this.destination = destination;
        setState(State.Created);
    }

    /**
     * Only used when streaming off the wire
     * @param destination
     * @param transactionId
     */
    protected AbstractMessage(RemoteNodeHandle destination, int transactionId, int originNodePort) {
        this.transactionId = transactionId;
        this.destination = destination;
        this.originNodePort = originNodePort;
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

    public int getOriginNodePort() {
        return originNodePort;
    }

    public void read(DataInputStream is) throws IOException {
        transactionId = is.readInt();
        originNodePort = is.readInt();
    }

    public void write(DataOutputStream os) throws IOException, FriendlyBackupException {
        os.writeInt(transactionId);
        os.writeInt(originNodePort);
    }

    public synchronized State getState() {
        return state;
    }

    public boolean isComplete() {
        return state == State.Finished || state == State.Error;
    }

    public synchronized void setState(State state) {
        synchronized(STATE_CHANGE_NOTIFIER) {
            this.state = state;
            STATE_CHANGE_NOTIFIER.notifyAll();
        }

        for(StateListener listener : stateListeners) {
            listener.stateChanged(this);
        }
    }

    public void addStateListener(
            StateListener listener) {
        stateListeners.add(listener);
    }

    public static void awaitStateChange() {
        synchronized (AbstractMessage.STATE_CHANGE_NOTIFIER) {
            try {
                AbstractMessage.STATE_CHANGE_NOTIFIER.wait();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static Message parseMessage(DataInputStream inputStream) throws IOException {
        int msgType = inputStream.readInt();
        MessageFactory factory = messageFactories.get(msgType);
        Message msg = factory.makeMessage();
        msg.read(inputStream);
        return msg;
    }

    public static void registerMessageFactory(int msgType, MessageFactory factory) {
        messageFactories.put(msgType, factory);
    }
}
