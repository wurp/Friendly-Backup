package com.geekcommune.friendlybackup.builder;

import java.util.Date;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.communication.ProgressWhenCompleteListener;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.main.ProgressTracker;
import com.geekcommune.identity.SecretIdentity;

public class LabelledDataBuilder {
    //private static final Logger log = Logger.getLogger(LabelledDataBuilder.class);

	public static LabelledData buildLabelledData(
	        SecretIdentity owner,
	        String label,
	        HashIdentifier id,
	        RemoteNodeHandle[] storingNodes,
	        int localPort,
	        Date expiryDate,
	        ProgressTracker progressTracker) throws FriendlyBackupException {
	    progressTracker.rebase(storingNodes.length);

		LabelledData labelledData =
		        new LabelledData(
		                owner,
		                label,
		                id);

		//for now, store the manifest on all storing nodes
		for(RemoteNodeHandle node : storingNodes) {
            VerifyMaybeSendDataMessage msg = new VerifyMaybeSendDataMessage(
                    node,
                    localPort,
                    labelledData.getHashID(), 
                    labelledData.toProto().toByteArray(),
                    new Lease(
                            expiryDate,
                            owner,
                            false,
                            labelledData.getHashID()));
            msg.addStateListener(new ProgressWhenCompleteListener(progressTracker, 1));
            BackupMessageUtil.instance().queueMessage(
                    msg);
		}

		return labelledData;
	}
}
