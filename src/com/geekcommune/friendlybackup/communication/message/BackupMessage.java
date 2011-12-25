package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractMessage;

public abstract class BackupMessage extends AbstractMessage {

    public BackupMessage(RemoteNodeHandle storingNode) {
        super(storingNode);
    }

    public static final String TYPE = "backup";
}
