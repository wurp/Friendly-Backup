package com.geekcommune.friendlybackup.communication;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.communication.message.BackupMessage;
import com.geekcommune.friendlybackup.communication.message.RetrieveDataMessage;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.datastore.DataStore;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.erasure.ErasureUtil;
import com.geekcommune.friendlybackup.format.low.Erasure;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.friendlybackup.main.ProgressTracker;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.identity.Signature;
import com.geekcommune.util.BinaryContinuation;
import com.geekcommune.util.Continuation;
import com.geekcommune.util.DateUtil;
import com.geekcommune.util.Pair;
import com.geekcommune.util.UnaryContinuation;
import com.google.protobuf.InvalidProtocolBufferException;

public class BackupMessageUtil extends MessageUtil {

    private static final Logger log = Logger.getLogger(BackupMessageUtil.class);

    private static final int NUM_THREADS = 10;
    private static final int MAX_TRIES = 5;

    private static BackupMessageUtil instance;

    public static BackupMessageUtil instance() {
        return instance;
    }

    public static void setInstance(BackupMessageUtil instance) {
        BackupMessageUtil.instance = instance;
    }

    protected BackupConfig bakcfg;
    private BlockingQueue<Runnable> workQueue;
    private Executor executor;
    private ConcurrentHashMap<RemoteNodeHandle, AtomicInteger> destinationFailures = new ConcurrentHashMap<RemoteNodeHandle, AtomicInteger>();
    private ConcurrentHashMap<RemoteNodeHandle, AtomicInteger> destinationSuccesses = new ConcurrentHashMap<RemoteNodeHandle, AtomicInteger>();

    public BackupMessageUtil() {
        workQueue = new LinkedBlockingQueue<Runnable>();
        executor = new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS, 1000, TimeUnit.MILLISECONDS, workQueue);
    }
    
    public BackupConfig getBackupConfig() {
        return bakcfg;
    }
    
    public void processBackupMessages(ProgressTracker progressTracker) throws ClassNotFoundException, SQLException {
        List<Message> msgs = DataStore.instance().getMessagesByType(BackupMessage.TYPE);
        //TODO create one message for each destination that batches all the ids of all objects
        //to be stored on that destination, and checks which of them actually need to be sent
        //then sends only those messages.
        for(Message msg : msgs) {
            final BackupMessage bakmsg = (BackupMessage) msg;
            //TODO somehow prefer to send to different hosts at the same time.
            //This sort of happens anyway since we pull messages back in the order
            //they were created, and they are created more or less round-robin by destination.
            reallyQueueMessage(bakmsg);
        }
        
        //TODO BOBBY figure out good way to wait for all messages to be processed.
    }

    private void reallyQueueMessage(final Message msg) {
        workQueue.add(new Runnable() {
            public void run() {
                send(msg);
            }
        });
    }

    protected void send(Message msg) {
        //TODO handle proxying somehow someday :-)
        Socket socket = null;
        try {
            socket = new Socket(msg.getDestination().getAddress(), msg.getDestination().getPort());
            socket.getOutputStream().write(msg.getDataToSend());
            socket.getOutputStream().flush();
            destinationSuccesses.putIfAbsent(msg.getDestination(), new AtomicInteger(0)).incrementAndGet();
        } catch (IOException e) {
            destinationFailures.putIfAbsent(msg.getDestination(), new AtomicInteger(0)).incrementAndGet();
            
            msg.setNumberOfTries(msg.getNumberOfTries() + 1);
            if( msg.getNumberOfTries() > MAX_TRIES ) {
                UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName());
                log.error("Not retrying, exceeded max tries: " + e.getMessage(), e);
            } else {
                UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName() + ", will retry", e);
                log.error("Failed to send message to " + msg.getDestination().getName() + ", will retry: " + e.getMessage(), e);
                
                reallyQueueMessage(msg);
            }
        } finally {
            if( socket != null ) {
                try {
                    socket.close();
                } catch (IOException e) {
                    UserLog.instance().logError("Failed to close socket; this may not actually be a problem...", e);
                    log.error(e.getMessage(), e);
                }
            }
            
            try {
                DataStore.instance().updateObject(msg);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                //TODO user log (this exception isn't thrown now)
            }
        }
    }

    public void cleanOutBackupMessageQueue() {
        try {
            DataStore.instance().deleteMessagesOfType(BackupMessage.TYPE);
        } catch (SQLException e) {
            UserLog.instance().logError("Failed to clean out backup message queue", e);
            log.error(e.getMessage(), e);
        }
        
    }

    /**
     * Pull in a piece of data, retrieving from any/all of storingNodes if necessary.
     * @param storingNodes 
     * @param id
     * @param continuation
     */
    public void retrieve(RemoteNodeHandle[] storingNodes, final HashIdentifier id, final Continuation continuation) {
        for(RemoteNodeHandle storingNode : storingNodes) {
            try {
                MessageUtil.instance().queueMessage(
                        new RetrieveDataMessage(
                                storingNode,
                                id, 
                                new UnaryContinuation<byte[]>() {

                                public void run(byte[] data) {
                                    try {
                                        DataStore.instance().storeData(id, data, new Lease(DateUtil.oneHourHence(), bakcfg.getOwner().getHandle(), Signature.INTERNAL_SELF_SIGNED));
                                        MessageUtil.instance().cancelListen(id);
                                        continuation.run();
                                    } catch (SQLException e) {
                                        log.error(e.getMessage(), e);
                                        UserLog.instance().logError("Failed to retrieve " + id, e);
                                    }
                                }
                            }));
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
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
        retrieve(storingNodes, id, new Continuation() {

            public void run() {
                try {
                    final LabelledData labelledData = LabelledData.fromProto(Basic.LabelledData.parseFrom(DataStore.instance().getData(id)));
                    final HashIdentifier erasureManifestId = labelledData.getPointingAt();
                    
                    log.info("Retrieved " + labelledData.getLabel());
                    
                    retrieve(storingNodes, erasureManifestId, new Continuation() {

                        public void run() {
                            try {
                                final ErasureManifest erasureManifest =
                                        ErasureManifest.fromProto(
                                                Basic.ErasureManifest.parseFrom(
                                                        DataStore.instance().getData(erasureManifestId)));
                                final List<Pair<HashIdentifier,RemoteNodeHandle>> retrievalData = erasureManifest.getRetrievalData();
                                final List<HashIdentifier> erasureIds = Pair.firstList(retrievalData);
                                
                                for(Pair<HashIdentifier, RemoteNodeHandle> retrievalDatum : retrievalData) {
                                    retrieve(retrievalDatum.getSecond(), retrievalDatum.getFirst(), new Continuation() {

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
                                            } catch (SQLException e) {
                                                log.error(e.getMessage(), e);
                                                UserLog.instance().logError("Failed to retrieve " + labelledData.getLabel(), e);
                                            }
                                        }
                                        
                                    });
                                }
                            } catch (InvalidProtocolBufferException e) {
                                log.error(e.getMessage(), e);
                                UserLog.instance().logError("Failed to retrieve " + labelledData.getLabel(), e);
                            } catch (SQLException e) {
                                log.error(e.getMessage(), e);
                                UserLog.instance().logError("Failed to retrieve " + labelledData.getLabel(), e);
                            }
                        }
                    });
                } catch (InvalidProtocolBufferException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError("Failed to retrieve " + id, e);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError("Failed to retrieve " + id, e);
                }
            }
        });
    }
    
    public void retrieve(
            RemoteNodeHandle node,
            final HashIdentifier id,
            final Continuation continuation) throws SQLException {
        MessageUtil.instance().queueMessage(
                new RetrieveDataMessage(
                        node,
                        id,
                        new UnaryContinuation<byte[]>() {

                        public void run(byte[] data) {
                            try {
                                DataStore.instance().storeData(id, data, new Lease(DateUtil.oneHourHence(), bakcfg.getOwner().getHandle(), Signature.INTERNAL_SELF_SIGNED));
                                continuation.run();
                            } catch (SQLException e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }));
    }

    public void setBackupConfig(BackupConfig bakcfg) {
        this.bakcfg = bakcfg;
    }
}
