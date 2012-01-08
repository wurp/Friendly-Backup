package com.geekcommune.friendlybackup.datastore;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public abstract class DataStore {
    private static DataStore instance;

    public static DataStore instance() {
        return instance ;
    }

    /**
     * Return a piece of data that should already be local.
     * @param id
     * @return
     * @throws SQLException 
     */
    public abstract byte[] getData(HashIdentifier id) throws SQLException;

    public void storeData(HashIdentifier id, byte[] data, Lease lease) throws SQLException {
        storeData(id, data, Collections.singletonList(lease));
    }

    public abstract void storeData(HashIdentifier id, byte[] data, List<Lease> leases) throws SQLException;
    
    public List<byte[]> getDataList(List<HashIdentifier> ids) throws SQLException {
        List<byte[]> retval = new ArrayList<byte[]>(ids.size());
        
        for(HashIdentifier id : ids) {
            byte[] data = getData(id);
            if( data != null ) {
                retval.add(data);
            }
        }
        
        return retval;
    }

    public abstract List<Lease> getLeases(HashIdentifier id) throws FriendlyBackupException;
    
    public static void setInstance(DataStore dataStore) {
        instance = dataStore;
    }
}
