package com.geekcommune.friendlybackup.communication.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.Message;
import com.geekcommune.communication.message.MessageFactory;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.proto.Basic;

/**
 * Message for transferring data requested by another node.
 * 
 * These messages are accumulated, then when we have them all (or a bunch of them), we build a list
 * of all the data ids for each storing node, and send them all in one batch.  The storing node
 * replies with the list of ids it doesn't already know about, and we send the data for those.
 * 
 * @author bobbym
 *
 */
public abstract class VerifyMaybeSendMessage extends BackupMessage {
    public static final MessageFactory FACTORY = new MessageFactory() {
        
        public Message makeMessage() {
            return new VerifyMaybeSendDataMessage();
        }
    };

    private HashIdentifier dataHashID;
    private Lease lease;
    
    public VerifyMaybeSendMessage(RemoteNodeHandle storingNode, int originNodePort, HashIdentifier dataHashID, Lease lease) {
        super(storingNode, originNodePort);
        this.dataHashID = dataHashID;
        this.lease = lease;
    }
    
    public VerifyMaybeSendMessage(int transactionId, RemoteNodeHandle storingNode, int originNodePort, HashIdentifier dataHashID, Lease lease) {
        super(transactionId, storingNode, originNodePort);
        this.dataHashID = dataHashID;
        this.lease = lease;
    }
    
    public VerifyMaybeSendMessage(int transactionId, int originNodePort, HashIdentifier dataHashID, Lease lease) {
        super(transactionId, originNodePort);
        this.dataHashID = dataHashID;
        this.lease = lease;
    }

    public HashIdentifier getDataHashID() {
        return dataHashID;
    }

    /**
     * Get the data to be stored
     * @return
     */
    public abstract byte[] getData();

    @Override
    public void read(DataInputStream is) throws IOException {
        super.read(is);

        this.dataHashID = HashIdentifier.fromProto(Basic.HashIdentifier.parseDelimitedFrom(is));
        //the corresponding code is in VerifyMaybeSendDataMessage.read
    }

    @Override
    public void write(DataOutputStream os) throws IOException {
        os.writeInt(getIntType());
        super.write(os);

        getDataHashID().toProto().writeDelimitedTo(os);
        
        //the corresponding code is in VerifyMaybeSendDataMessage.read
        byte[] data = getData();
        os.writeInt(data.length);
        os.write(data);
    }

    protected abstract int getIntType();

    public Lease getLease() {
        return lease;
    }
}
