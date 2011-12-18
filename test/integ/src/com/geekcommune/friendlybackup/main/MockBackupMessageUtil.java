package com.geekcommune.friendlybackup.main;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.communication.message.RetrieveDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendMessage;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.util.Pair;

public class MockBackupMessageUtil extends BackupMessageUtil {
    private Map<Pair<RemoteNodeHandle,HashIdentifier>, byte[]> dataSent = new HashMap<Pair<RemoteNodeHandle,HashIdentifier>, byte[]>();

    public MockBackupMessageUtil() {
    }
    
    @Override
    public void processBackupMessages(ProgressTracker progressTracker) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void cleanOutBackupMessageQueue() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void queueMessages(RemoteNodeHandle[] remoteNodeHandles,
            Message msg) {
        for(RemoteNodeHandle sn : remoteNodeHandles) {
            queueMessage(sn, msg);
        }
    }

    @Override
    public void cancelListen(List<HashIdentifier> ids) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void queueMessage(RemoteNodeHandle storingNode,
            Message msg) {
        if( msg instanceof VerifyMaybeSendMessage ) {
            VerifyMaybeSendMessage vmsm = (VerifyMaybeSendMessage) msg;
            dataSent.put(new Pair<RemoteNodeHandle,HashIdentifier>(storingNode, ((VerifyMaybeSendMessage) msg).getDataHashID()), vmsm.getDataToSend());
        } else if( msg instanceof RetrieveDataMessage ){
            RetrieveDataMessage rdm = (RetrieveDataMessage) msg;
            byte[] data = dataSent.get(new Pair<RemoteNodeHandle,HashIdentifier>(storingNode, rdm.getHashID()));
            rdm.handleResponse(data);
        } else {
            System.out.println("Unhandled message " + msg);
        }
    }
}
