package com.geekcommune.communication;

import java.sql.SQLException;
import java.util.List;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.datastore.DataStore;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class MessageUtil {

	private static MessageUtil instance = new MessageUtil();

    public static MessageUtil instance() {
		return instance;
	}

    public final void cancelListen(List<HashIdentifier> ids) {
        for(HashIdentifier id : ids) {
            cancelListen(id);
        }
    }

    public static void setInstance(MessageUtil messageUtil) {
        instance = messageUtil;
    }

    public void queueMessage(
            Message msg) throws SQLException {
        DataStore.instance().addMessage(msg);
    }

    public void cancelListen(HashIdentifier id) {
        // TODO Auto-generated method stub
        
    }

}
