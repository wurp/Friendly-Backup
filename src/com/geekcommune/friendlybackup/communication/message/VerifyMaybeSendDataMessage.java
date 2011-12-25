package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class VerifyMaybeSendDataMessage extends VerifyMaybeSendMessage {
    private byte[] data;
    private Lease lease;

    public VerifyMaybeSendDataMessage(RemoteNodeHandle destination, HashIdentifier dataHashID, byte[] data, Lease lease) {
        super(destination, dataHashID);
        this.data = data;
        this.lease = lease;
    }

    @Override
    public byte[] getDataToSend() {
        //TODO send lease
        return data;
    }
}
