package com.geekcommune.friendlybackup.communication.message;

import java.io.DataInputStream;
import java.io.IOException;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
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
    public byte[] getData() throws FriendlyBackupException {
        Erasure erasure = new Erasure(erasureFinder.getErasure(idx), idx);
        
        //make sure the erasure we got has the id it's supposed to...
        if( !erasure.getHashID().equals(getDataHashID()) ) {
            throw new FriendlyBackupException(
                    "Expected erasure to have id " +
                    getDataHashID() +
                    ", but it had id " +
                    erasure.getHashID());
        }

        return erasure.toProto().toByteArray();
    }

    @Override
    public final int getType() {
        return INT_TYPE;
    }

    @Override
    protected final void internalSendDataRead(DataInputStream is) throws IOException,
            FriendlyBackupException {
        throw new FriendlyBackupException("Reading " + getClass() + " is not supported");
    }
}
