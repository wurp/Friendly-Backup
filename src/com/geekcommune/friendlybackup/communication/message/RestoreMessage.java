package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractMessage;

public abstract class RestoreMessage extends AbstractMessage {

    public RestoreMessage(RemoteNodeHandle storingNode, int originNodePort) {
        super(storingNode, originNodePort);
    }
    
    /**
     * Only used when streaming off the wire
     * @param transactionId
     */
    protected RestoreMessage(int transactionId, int originNodePort) {
        super(transactionId, originNodePort);
    }

}
