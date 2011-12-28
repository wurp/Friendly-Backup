package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.erasure.ErasureFinder;
import com.geekcommune.friendlybackup.format.low.Erasure;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class VerifyMaybeSendErasureMessage extends VerifyMaybeSendMessage {
    public static final int INT_TYPE = 5;

    private ErasureFinder erasureFinder;
    private int idx;

    public VerifyMaybeSendErasureMessage(
            RemoteNodeHandle storingNode,
            int originNodePort,
            HashIdentifier erasureDataId,
            ErasureFinder erasureFinder,
            int idx,
            Lease lease) {
        super(storingNode, originNodePort, erasureDataId, lease);
        this.erasureFinder = erasureFinder;
        this.idx = idx;
    }

    @Override
    public byte[] getData() {
        return new Erasure(erasureFinder.getErasure(idx), idx).toProto().toByteArray();
    }

    @Override
    protected int getIntType() {
        return INT_TYPE;
    }
}
