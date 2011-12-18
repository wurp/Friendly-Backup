package com.geekcommune.friendlybackup.builder;

import java.util.Date;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.identity.PrivateIdentity;

public class LabelledDataBuilder {

	public static LabelledData buildLabelledData(PrivateIdentity owner, String label, HashIdentifier id, RemoteNodeHandle[] storingNodes, Date expiryDate) {
		LabelledData labelledData = new LabelledData(owner, label, id);
		
		//for now, store the manifest on all storing nodes
		for(RemoteNodeHandle node : storingNodes) {
			MessageUtil.instance().queueMessage(
			        node,
			        new VerifyMaybeSendDataMessage(
			                labelledData.getHashID(), 
			                labelledData.toProto().toByteArray()));
		}

		return labelledData;
	}
}
