package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractMessage;

public abstract class RestoreMessage extends AbstractMessage {

    public final String TYPE = "restore";

    public RestoreMessage(RemoteNodeHandle storingNode) {
        super(storingNode);
    }
    
    public byte[] getDataToSend() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getType() {
        return TYPE;
    }

}
