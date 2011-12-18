package com.geekcommune.communication;

import java.util.List;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.format.Data;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.util.UnaryContinuation;

public class MessageUtil {

	private static MessageUtil instance = new MessageUtil();

    public static MessageUtil instance() {
		return instance;
	}

    public void queueMessages(RemoteNodeHandle[] remoteNodeHandles,
            Message message) {
        // TODO Auto-generated method stub
        
    }

    public void cancelListen(List<HashIdentifier> ids) {
        // TODO Auto-generated method stub
        
    }

    public static void setInstance(MessageUtil messageUtil) {
        instance = messageUtil;
    }

    public void queueMessage(RemoteNodeHandle storingNode,
            Message msg) {
        // TODO Auto-generated method stub
        
    }

}
