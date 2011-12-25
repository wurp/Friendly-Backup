package com.geekcommune.communication.message;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.handler.Handler;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class RetrieveLabelledFileMessage extends AbstractMessage {

    public RetrieveLabelledFileMessage(RemoteNodeHandle destination, HashIdentifier first) {
        super(destination);
        // TODO Auto-generated constructor stub
    }

    public Handler getHandler() {
        // TODO Auto-generated method stub
        return null;
    }

    public void setHandler(Handler chainHandlers) {
        // TODO Auto-generated method stub
        
    }

    public byte[] getDataToSend() {
        // TODO Auto-generated method stub
        return null;
    }

}
