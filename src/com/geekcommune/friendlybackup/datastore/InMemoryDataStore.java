package com.geekcommune.friendlybackup.datastore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;

public class InMemoryDataStore extends DataStore {
    //private static final Logger log = Logger.getLogger(InMemoryDataStore.class);

    private ConcurrentHashMap<HashIdentifier, byte[]> dataMap = new ConcurrentHashMap<HashIdentifier, byte[]>();
    private ConcurrentHashMap<HashIdentifier, List<Lease>> leaseMap = new ConcurrentHashMap<HashIdentifier, List<Lease>>();
    
    @Override
    public byte[] getData(HashIdentifier id) {
        return dataMap.get(id);
    }

    @Override
    public void storeData(HashIdentifier id, byte[] data, List<Lease> leases) {
        dataMap.put(id, data);
        
        leaseMap.putIfAbsent(id, Collections.synchronizedList(new ArrayList<Lease>()));
        leaseMap.get(id).addAll(leases);
    }

    @Override
    public List<Lease> getLeases(HashIdentifier id)
            throws FriendlyBackupException {
        return leaseMap.get(id);
    }
}
