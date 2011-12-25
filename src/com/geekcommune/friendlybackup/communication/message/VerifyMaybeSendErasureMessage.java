package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.erasure.ErasureFinder;
import com.geekcommune.friendlybackup.format.low.Erasure;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.onionnetworks.util.Buffer;

public class VerifyMaybeSendErasureMessage extends VerifyMaybeSendMessage {
    private ErasureFinder erasureFinder;
    private int idx;
    private Lease lease;

    public VerifyMaybeSendErasureMessage(RemoteNodeHandle storingNode, HashIdentifier erasureDataId,
            ErasureFinder erasureFinder, int idx, Lease lease) {
        super(storingNode, erasureDataId);
        this.erasureFinder = erasureFinder;
        this.idx = idx;
        this.lease = lease;
    }
    
    public byte[] getDataToSend() {
        Buffer buffer = erasureFinder.getErasure(idx);
        Erasure erasure = new Erasure(buffer, idx);
        return erasure.toProto().toByteArray();
    }
}
