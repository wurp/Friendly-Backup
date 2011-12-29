package com.geekcommune.communication;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class MessageUtil {

	private static MessageUtil instance = new MessageUtil();

    public static MessageUtil instance() {
		return instance;
	}

    public static void setInstance(MessageUtil messageUtil) {
        instance = messageUtil;
    }

    public void queueMessage(
            Message msg) {
        msg.setState(Message.State.NeedsProcessing);
    }

    public void cancelListen(HashIdentifier id) {
        // TODO Auto-generated method stub
        
    }

}
