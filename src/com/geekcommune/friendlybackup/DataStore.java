package com.geekcommune.friendlybackup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.communication.message.RetrieveDataMessage;
import com.geekcommune.friendlybackup.erasurefinder.ErasureUtil;
import com.geekcommune.friendlybackup.erasurefinder.UserLog;
import com.geekcommune.friendlybackup.format.low.BufferData;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.util.BinaryContinuation;
import com.geekcommune.util.Continuation;
import com.geekcommune.util.Pair;
import com.geekcommune.util.UnaryContinuation;
import com.google.protobuf.InvalidProtocolBufferException;

public class DataStore {
    private static final Logger log = Logger.getLogger(DataStore.class);

    private static DataStore instance = new DataStore();
    private Map<HashIdentifier, byte[]> dataMap;

    protected DataStore() {
        this.dataMap = new HashMap<HashIdentifier,byte[]>();
    }
    
    public static DataStore instance() {
        return instance ;
    }

    /**
     * Pull in a piece of data, retrieving from any/all of storingNodes if necessary.
     * @param storingNodes 
     * @param id
     * @param continuation
     */
    public void retrieve(RemoteNodeHandle[] storingNodes, final HashIdentifier id, final Continuation continuation) {
        MessageUtil.instance().queueMessages(
                storingNodes,
                new RetrieveDataMessage(id, 
                        new UnaryContinuation<byte[]>() {

                        public void run(byte[] data) {
                            storeData(id, data);
                            continuation.run();
                        }
                    }));
    }

    /**
     * Pull in a piece of data, retrieving from any/all of storingNodes if necessary.
     * @param storingNodes 
     * @param id
     * @param continuation
     */
    public void retrieveLabelledData(final RemoteNodeHandle[] storingNodes, final HashIdentifier id, final BinaryContinuation<String,byte[]> continuation) {
        DataStore.instance().retrieve(storingNodes, id, new Continuation() {

            public void run() {
                try {
                    final LabelledData labelledData = LabelledData.fromProto(Basic.LabelledData.parseFrom(DataStore.instance().getData(id)));
                    final HashIdentifier erasureManifestId = labelledData.getPointingAt();
                    
                    DataStore.instance().retrieve(storingNodes, erasureManifestId, new Continuation() {

                        public void run() {
                            try {
                                final ErasureManifest erasureManifest =
                                        ErasureManifest.fromProto(
                                                Basic.ErasureManifest.parseFrom(
                                                        DataStore.instance().getData(erasureManifestId)));
                                final List<Pair<HashIdentifier,RemoteNodeHandle>> retrievalData = erasureManifest.getRetrievalData();
                                final List<HashIdentifier> erasureIds = Pair.firstList(retrievalData);
                                
                                for(Pair<HashIdentifier, RemoteNodeHandle> retrievalDatum : retrievalData) {
                                    DataStore.instance().retrieve(retrievalDatum.getSecond(), retrievalDatum.getFirst(), new Continuation() {

                                        public void run() {
                                            try {
                                                //check if enough of the blocks have come in to reconstitute the data
                                                List<byte[]> dataList = DataStore.instance().getDataList(erasureIds);
                                                
                                                if( dataList.size() >= erasureManifest.getErasuresNeeded() ) {
                                                    //stop listening for any other blocks to come in
                                                    MessageUtil.instance().cancelListen(erasureIds);
                                                    
                                                    //reconstitute the erasure blocks into the original data
                                                    List<BufferData> erasures = new ArrayList<BufferData>(dataList.size());
                                                    for(byte[] erasureObj : dataList) {
                                                        //TODO no need to bail on decoding altogether if parse throws exception; could
                                                        //wait for more erasures we get enough that work OR are sure we will never get enough
                                                        BufferData erasure = BufferData.fromProto(Basic.Erasure.parseFrom(erasureObj));
                                                        erasure.setIndex(erasureManifest.getIndex(erasure.getHashID()));
                                                        erasures.add(erasure);
                                                    }
                                                    
                                                    byte[] fullContents = new byte[erasureManifest.getContentSize()];
                                                    ErasureUtil.decode(
                                                            fullContents,
                                                            erasureManifest.getErasuresNeeded(),
                                                            erasureManifest.getTotalErasures(),
                                                            erasures);

                                                    continuation.run(labelledData.getLabel(), fullContents);
                                                }
                                            } catch (InvalidProtocolBufferException e) {
                                                log.error(e.getMessage(), e);
                                                UserLog.instance().logError("Failed to retrieve " + labelledData.getLabel(), e);
                                            }
                                        }
                                        
                                    });
                                }
                            } catch (InvalidProtocolBufferException e) {
                                log.error(e.getMessage(), e);
                                UserLog.instance().logError("Failed to retrieve " + labelledData.getLabel(), e);
                            }
                        }
                    });
                } catch (InvalidProtocolBufferException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError("Failed to retrieve " + id, e);
                }
            }
        });
    }

    /**
     * Return a piece of data that should already be local.
     * @param id
     * @return
     */
    public byte[] getData(HashIdentifier id) {
        // TODO long term solution?
        return dataMap.get(id);
    }

    public void storeData(HashIdentifier id, byte[] data) {
        // TODO long term solution?
        dataMap.put(id, data);
    }
    
    public void retrieve(RemoteNodeHandle node, final HashIdentifier id,
            Continuation continuation) {
        MessageUtil.instance().queueMessage(
                node,
                new RetrieveDataMessage(id,
                        new UnaryContinuation<byte[]>() {

                        public void run(byte[] data) {
                            storeData(id, data);
                        }
                    }));
    }

    public List<byte[]> getDataList(List<HashIdentifier> ids) {
        List<byte[]> retval = new ArrayList<byte[]>(ids.size());
        
        for(HashIdentifier id : ids) {
            retval.add(getData(id));
        }
        
        return retval;
    }

}
