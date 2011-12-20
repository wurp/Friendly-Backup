package com.geekcommune.friendlybackup.communication.message;

import com.geekcommune.communication.message.AbstractMessage;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

/**
 * These messages are accumulated, then when we have them all (or a bunch of them), we build a list
 * of all the data ids for each storing node, and send them all in one batch.  The storing node
 * replies with the list of ids it doesn't already know about, and we send the data for those.
 * 
 * @author bobbym
 *
 */
public abstract class VerifyMaybeSendMessage extends AbstractMessage {
    private HashIdentifier dataHashID;
    
    public VerifyMaybeSendMessage(HashIdentifier dataHashID) {
        this.dataHashID = dataHashID;
    }
    
    public HashIdentifier getDataHashID() {
        return dataHashID;
    }

    public abstract byte[] getDataToSend();
}
