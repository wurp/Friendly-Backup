package com.geekcommune.friendlybackup.datastore;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class InMemoryDataStore extends DataStore {
    private static final Logger log = Logger.getLogger(InMemoryDataStore.class);

    private ConcurrentHashMap<HashIdentifier, byte[]> dataMap = new ConcurrentHashMap<HashIdentifier, byte[]>();
    
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

    @Override
    public List<Message> getMessagesByType(String type) throws SQLException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateObject(Message msg) throws SQLException {
        //in-memory objects are already updated; do nothing.
    }

    @Override
    public void deleteMessagesOfType(String type) throws SQLException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void addMessage(Message msg) throws SQLException {
        // TODO Auto-generated method stub
        
    }
}
