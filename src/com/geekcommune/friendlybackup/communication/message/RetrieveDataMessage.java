package com.geekcommune.friendlybackup.communication.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.Message;
import com.geekcommune.communication.message.MessageFactory;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.util.UnaryContinuation;

public class RetrieveDataMessage extends RestoreMessage {

    public static final int INT_TYPE = 3;
    public static final MessageFactory FACTORY = new MessageFactory() {
        
        public Message makeMessage() {
            return new RetrieveDataMessage(0, 0, null);
        }
    };

    private HashIdentifier id;
    private UnaryContinuation<byte[]> responseHandler;

    public RetrieveDataMessage(RemoteNodeHandle destination, int originNodePort, HashIdentifier id, UnaryContinuation<byte[]> responseHandler) {
        super(destination, originNodePort);
        this.id = id;
        this.responseHandler = responseHandler;
    }

    /**
     * Only used when streaming off the wire
     * @param transactionId
     */
    protected RetrieveDataMessage(int transactionId, int originNodePort, HashIdentifier id) {
        super(transactionId, originNodePort);
        this.id = id;
    }

    public UnaryContinuation<byte[]> getResponseHandler() {
        return responseHandler;
    }
    
    public HashIdentifier getHashIDOfDataToRetrieve() {
        return id;
    }

    public void handleResponse(byte[] data) {
        responseHandler.run(data);
    }

    @Override
    protected final void internalRead(DataInputStream is) throws IOException, FriendlyBackupException {
        id = HashIdentifier.fromProto(Basic.HashIdentifier.parseDelimitedFrom(is));
    }

    @Override
    protected final void internalWrite(DataOutputStream os) throws IOException, FriendlyBackupException {
        id.toProto().writeDelimitedTo(os);
    }

	@Override
	public final int getType() {
		return INT_TYPE;
	}
}
