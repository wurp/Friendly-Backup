package com.geekcommune.friendlybackup.datastore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.communication.message.RetrieveDataMessage;
import com.geekcommune.friendlybackup.erasure.ErasureUtil;
import com.geekcommune.friendlybackup.format.low.Erasure;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.logging.UserLog;
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
                            MessageUtil.instance().cancelListen(id);
                            storeData(id, data);
                            continuation.run();
                        }
                    }));
    }

    /**
     * Pull in a piece of data, retrieving from any/all of storingNodes if necessary.  Also
     * resolves erasure manifest, retrieving the chunks necessary to reconstitute the original data
     * from the erasures.
     * 
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
                    
                    log.info("Retrieved " + labelledData.getLabel());
                    
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
                                                    List<com.geekcommune.friendlybackup.erasure.Erasure> erasures =
                                                            new ArrayList<com.geekcommune.friendlybackup.erasure.Erasure>(dataList.size());
                                                    for(byte[] erasureObj : dataList) {
                                                        //TODO no need to bail on decoding altogether if parse throws exception; could
                                                        //wait for more erasures we get enough that work OR are sure we will never get enough
                                                        Erasure erasure = Erasure.fromProto(Basic.Erasure.parseFrom(erasureObj));
                                                        erasure.setIndex(erasureManifest.getIndex(erasure.getHashID()));
                                                        erasures.add(erasure.getPlainErasure());
                                                    }
                                                    
                                                    byte[] fullContents = new byte[erasureManifest.getContentSize()];
                                                    ErasureUtil.decode(
                                                            fullContents,
                                                            erasureManifest.getErasuresNeeded(),
                                                            erasureManifest.getTotalErasures(),
                                                            erasures);
                                                    log.info("Rebuilt contents of " + labelledData.getLabel());

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
        byte[] retval = dataMap.get(id);
        log.info("Got " + (retval == null ? null : retval.length) + " bytes for " + id);
        return retval;
    }

    public void storeData(HashIdentifier id, byte[] data) {
        // TODO long term solution?
        dataMap.put(id, data);
        log.info("Writing " + (data == null ? null : data.length) + " bytes for " + id);
     }
    
    public void retrieve(
            RemoteNodeHandle node,
            final HashIdentifier id,
            final Continuation continuation) {
        MessageUtil.instance().queueMessage(
                node,
                new RetrieveDataMessage(id,
                        new UnaryContinuation<byte[]>() {

                        public void run(byte[] data) {
                            storeData(id, data);
                            continuation.run();
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
