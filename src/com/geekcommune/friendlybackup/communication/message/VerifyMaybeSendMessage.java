package com.geekcommune.friendlybackup.communication.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.Message;
import com.geekcommune.communication.message.MessageFactory;
import com.geekcommune.friendlybackup.FriendlyBackupException;
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
     * @throws FriendlyBackupException 
     */
    public abstract byte[] getData() throws FriendlyBackupException;

    @Override
    protected final void internalRead(DataInputStream is) throws IOException, FriendlyBackupException {
        this.dataHashID = HashIdentifier.fromProto(Basic.HashIdentifier.parseDelimitedFrom(is));
        
        this.lease = Lease.fromProto(Basic.Lease.parseDelimitedFrom(is));
        
        //the corresponding code is in VerifyMaybeSendDataMessage.read
        internalSendDataRead(is);
    }

    protected abstract void internalSendDataRead(DataInputStream is)  throws IOException, FriendlyBackupException;

	@Override
    protected final void internalWrite(DataOutputStream os) throws IOException, FriendlyBackupException {
        getDataHashID().toProto().writeDelimitedTo(os);
        
        Lease lease = getLease();
        Basic.Lease leaseProto = lease.toProto();
        leaseProto.writeDelimitedTo(os);
        
        //the corresponding code is in VerifyMaybeSendDataMessage.read
        byte[] data = getData();
        os.writeInt(data.length);
        os.write(data);
    }

    public Lease getLease() {
        return lease;
    }
}
