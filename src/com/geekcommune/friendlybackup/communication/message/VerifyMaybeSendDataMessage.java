package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class VerifyMaybeSendDataMessage extends VerifyMaybeSendMessage {
    private byte[] data;

    public VerifyMaybeSendDataMessage(HashIdentifier dataHashID, byte[] data) {
        super(dataHashID);
        this.data = data;
    }

    @Override
    public byte[] getDataToSend() {
        return data;
    }

}
