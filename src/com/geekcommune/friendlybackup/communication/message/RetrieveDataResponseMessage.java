package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractMessage;

public class RetrieveDataResponseMessage extends AbstractMessage {

    public static final String TYPE = "restoreResponse";

    public RetrieveDataResponseMessage(RemoteNodeHandle storingNode, long transactionId) {
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
