package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractMessage;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class SendDataMessage extends AbstractMessage {

    private HashIdentifier id;
    private byte[] data;

    public SendDataMessage(RemoteNodeHandle destination, HashIdentifier id, byte[] data) {
        super(destination);
        this.id = id;
        this.data = data;
    }

    public byte[] getDataToSend() {
        return null;
    }

}
