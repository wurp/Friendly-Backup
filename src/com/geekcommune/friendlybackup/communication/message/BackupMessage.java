package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractMessage;

public abstract class BackupMessage extends AbstractMessage {

    public BackupMessage(RemoteNodeHandle storingNode, int originNodePort) {
        super(storingNode, originNodePort);
    }

    public BackupMessage(int transactionId, RemoteNodeHandle storingNode, int originNodePort) {
        super(transactionId, storingNode, originNodePort);
    }

    public BackupMessage(int transactionId, int originNodePort) {
        super(transactionId, originNodePort);
    }
}
