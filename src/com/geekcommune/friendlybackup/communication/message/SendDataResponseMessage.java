package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractMessage;

public class SendDataResponseMessage extends AbstractMessage {

    public static final String TYPE = "backupResponse";

    public SendDataResponseMessage(
            RemoteNodeHandle storingNode,
            int transactionId) {
        super(storingNode, transactionId);
    }

    public byte[] getDataToSend() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getType() {
        return TYPE;
    }

}
