package com.geekcommune.friendlybackup.builder;

import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.communication.ProgressWhenCompleteListener;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.main.ProgressTracker;
import com.geekcommune.identity.PrivateIdentity;

public class LabelledDataBuilder {
    private static final Logger log = Logger.getLogger(LabelledDataBuilder.class);

	public static LabelledData buildLabelledData(
	        PrivateIdentity owner,
	        String label,
	        HashIdentifier id,
	        RemoteNodeHandle[] storingNodes,
	        int localPort,
	        Date expiryDate,
	        ProgressTracker progressTracker) {
	    progressTracker.rebase(storingNodes.length);

		LabelledData labelledData = new LabelledData(owner, label, id);

		//for now, store the manifest on all storing nodes
		for(RemoteNodeHandle node : storingNodes) {
			try {
                VerifyMaybeSendDataMessage msg = new VerifyMaybeSendDataMessage(
                        node,
                        localPort,
                        labelledData.getHashID(), 
                        labelledData.toProto().toByteArray(),
                        owner.makeLease(labelledData.getHashID(), expiryDate));
                msg.addStateListener(new ProgressWhenCompleteListener(progressTracker, 1));
                MessageUtil.instance().queueMessage(
                        msg);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
		}

		return labelledData;
	}
}
