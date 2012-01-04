package com.geekcommune.friendlybackup.datastore;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class InMemoryDataStore extends DataStore {
    //private static final Logger log = Logger.getLogger(InMemoryDataStore.class);

    private ConcurrentHashMap<HashIdentifier, byte[]> dataMap = new ConcurrentHashMap<HashIdentifier, byte[]>();
    //private ConcurrentHashMap<String, List<Message>> messagesByType = new ConcurrentHashMap<String, List<Message>>();
    
    @Override
    public byte[] getData(HashIdentifier id) throws SQLException {
        return dataMap.get(id);
    }

    @Override
    public void storeData(HashIdentifier id, byte[] data, List<Lease> leases)
            throws SQLException {
        dataMap.put(id, data);
        //TODO leases are ignored for now
    }
}
