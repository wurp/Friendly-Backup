package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.AbstractMessage;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.util.UnaryContinuation;

public class RetrieveDataMessage extends AbstractMessage {

    private HashIdentifier id;
    private UnaryContinuation<byte[]> responseHandler;

    public RetrieveDataMessage(RemoteNodeHandle destination, HashIdentifier id, UnaryContinuation<byte[]> responseHandler) {
        super(destination);
        this.id = id;
        this.responseHandler = responseHandler;
    }

    public HashIdentifier getHashIDOfDataToRetrieve() {
        return id;
    }

    public void handleResponse(byte[] data) {
        responseHandler.run(data);
    }

    public byte[] getDataToSend() {
        // TODO Auto-generated method stub
        return null;
    }
}
