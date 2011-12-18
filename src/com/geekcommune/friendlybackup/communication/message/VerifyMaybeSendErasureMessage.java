package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.friendlybackup.erasurefinder.ErasureFinder;
import com.geekcommune.friendlybackup.format.low.BufferData;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.onionnetworks.util.Buffer;

public class VerifyMaybeSendErasureMessage extends VerifyMaybeSendMessage {
    private ErasureFinder erasureFinder;
    private int idx;

    public VerifyMaybeSendErasureMessage(HashIdentifier erasureDataId,
            ErasureFinder erasureFinder, int idx) {
        super(erasureDataId);
        this.erasureFinder = erasureFinder;
        this.idx = idx;
    }
    
    public byte[] getDataToSend() {
        Buffer buffer = erasureFinder.getErasure(idx);
        BufferData erasure = new BufferData(buffer, idx);
        return erasure.toProto().toByteArray();
    }
}