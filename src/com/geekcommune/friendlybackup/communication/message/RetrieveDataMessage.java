package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.util.UnaryContinuation;

public class RetrieveDataMessage implements Message {

    private HashIdentifier id;
    private UnaryContinuation<byte[]> responseHandler;

    public RetrieveDataMessage(HashIdentifier id, UnaryContinuation<byte[]> responseHandler) {
        this.id = id;
        this.responseHandler = responseHandler;
    }

    public HashIdentifier getHashID() {
        return id;
    }

    public void handleResponse(byte[] data) {
        responseHandler.run(data);
    }
}
