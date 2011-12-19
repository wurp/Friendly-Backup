package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class SendDataMessage implements Message {

    private HashIdentifier id;
    private byte[] data;

    public SendDataMessage(HashIdentifier id, byte[] data) {
        this.id = id;
        this.data = data;
    }

}
