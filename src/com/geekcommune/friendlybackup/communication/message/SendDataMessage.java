package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.message.AbstractMessage;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class SendDataMessage extends AbstractMessage {

    private HashIdentifier id;
    private byte[] data;

    public SendDataMessage(HashIdentifier id, byte[] data) {
        this.id = id;
        this.data = data;
    }

}
