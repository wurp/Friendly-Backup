package com.geekcommune.friendlybackup.main;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.communication.message.RetrieveDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendMessage;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.datastore.DataStore;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.identity.Signature;
import com.geekcommune.util.DateUtil;
import com.geekcommune.util.Pair;

public class MockBackupMessageUtil extends BackupMessageUtil {
    private static final Logger log = Logger.getLogger(MockBackupMessageUtil.class);

    private Map<Pair<RemoteNodeHandle,HashIdentifier>, byte[]> dataSent = new HashMap<Pair<RemoteNodeHandle,HashIdentifier>, byte[]>();

    private List<HashIdentifier> dontListenList = new ArrayList<HashIdentifier>();

    public MockBackupMessageUtil(BackupConfig backupConfig) {
        this.bakcfg = backupConfig;
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
    public void cancelListen(HashIdentifier id) {
        dontListenList.add(id);
    }

    @Override
    public void queueMessage(
            Message msg) {
        if( msg instanceof VerifyMaybeSendMessage ) {
            VerifyMaybeSendMessage vmsm = (VerifyMaybeSendMessage) msg;
            Pair<RemoteNodeHandle,HashIdentifier> key = new Pair<RemoteNodeHandle,HashIdentifier>(msg.getDestination(), vmsm.getDataHashID());
            log.info("Putting " + vmsm.getData().length + " bytes for key " + key);
            dataSent.put(key, vmsm.getData());
            
            try {
                DataStore.instance().storeData(
                        vmsm.getDataHashID(),
                        vmsm.getData(),
                        new Lease(
                                DateUtil.oneHourHence(),
                                getBackupConfig().getOwner().getHandle(),
                                Signature.INTERNAL_SELF_SIGNED));
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        } else if( msg instanceof RetrieveDataMessage ){
            RetrieveDataMessage rdm = (RetrieveDataMessage) msg;
            
            if( dontListenList.contains(rdm.getHashIDOfDataToRetrieve()) ) {
                log.info("not listening to " + rdm.getHashIDOfDataToRetrieve());
            } else {
                Pair<RemoteNodeHandle,HashIdentifier> key = new Pair<RemoteNodeHandle,HashIdentifier>(msg.getDestination(), rdm.getHashIDOfDataToRetrieve());
                byte[] data = dataSent.get(key);
                log.info("Retrieved " + (data == null ? null : data.length) + " bytes for key " + key);
                
                if( data == null ) {
                    try {
                        byte[] dsdata = DataStore.instance().getData(rdm.getHashIDOfDataToRetrieve());
                        log.info("Retrieved " + (dsdata == null ? null : dsdata.length) + " bytes for key " + rdm.getHashIDOfDataToRetrieve());
                        rdm.handleResponse(dsdata);
                    } catch (SQLException e) {
                        log.error(e.getMessage(), e);
                    }
                } else {
                    rdm.handleResponse(data);
                }
            }
        } else {
            System.out.println("Unhandled message " + msg);
        }
    }
}
