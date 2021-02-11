package com.geekcommune.friendlybackup.communication.message;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class VerifyMaybeSendDataMessage extends VerifyMaybeSendMessage {
    public static final int INT_TYPE = 1;

    private byte[] data;

    public VerifyMaybeSendDataMessage(RemoteNodeHandle destination, int originNodePort, HashIdentifier dataHashID, byte[] data, Lease lease) {
        super(destination, originNodePort, dataHashID, lease);
        this.data = data;
    }

    public VerifyMaybeSendDataMessage(RemoteNodeHandle destination, int transactionId, int originNodePort, HashIdentifier dataHashID, byte[] data, Lease lease) {
        super(transactionId, destination, originNodePort, dataHashID, lease);
        this.data = data;
    }

    protected VerifyMaybeSendDataMessage() {
        super(0, 0, null, null);
    }

    @Override
    public byte[] getData() {
        return data;
    }

    //the write method is fully in the superclass
    @Override
    protected final void internalSendDataRead(DataInputStream is) throws IOException, FriendlyBackupException {
        int len = is.readInt();
        
        if( len != 0 ) {
            data = new byte[len];
            readBytes(is, data, 0, data.length);
//            if( data.length != bytesRead ) {
//                int remaining = is.read();
//                throw new RuntimeException("Could not read all " + data.length + " bytes, found " + bytesRead + ", found " + remaining + " when looking for more data");
//            }
        }
    }

    private void readBytes(InputStream is, byte[] data, int startIdx, int length) throws IOException {
        int total = 0;
        while( total < length ) {
            int lastRead = is.read(data, startIdx + total, length - total);
            if( lastRead == -1 ) {
                throw new RuntimeException("Could not read all " + length + " bytes, found " + total + ", before hitting end of stream");
            }
            total += lastRead;
        }
    }

    @Override
    public final int getType() {
        return INT_TYPE;
    }
}
