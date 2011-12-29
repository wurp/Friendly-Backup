package com.geekcommune.communication;

import java.sql.SQLException;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.datastore.DataStore;
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
            Message msg) throws SQLException {
        msg.setState(Message.State.NeedsProcessing);
        DataStore.instance().addMessage(msg);
    }

    public void cancelListen(HashIdentifier id) {
        // TODO Auto-generated method stub
        
    }

}
