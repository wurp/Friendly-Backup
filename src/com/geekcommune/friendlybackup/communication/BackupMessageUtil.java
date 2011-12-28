package com.geekcommune.friendlybackup.communication;


import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.communication.message.BackupMessage;
import com.geekcommune.friendlybackup.communication.message.RetrieveDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.datastore.DataStore;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.erasure.ErasureUtil;
import com.geekcommune.friendlybackup.format.low.Erasure;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.logging.UserLog;
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

    /**
     * Map from transaction id to the continuation for handling the data requested in that xaction
     */
    private ConcurrentHashMap<Integer, UnaryContinuation<byte[]>> responseHandlers = new ConcurrentHashMap<Integer, UnaryContinuation<byte[]>>();

    private ConcurrentHashMap<Pair<InetAddress, Integer>, Socket> socketMap = new ConcurrentHashMap<Pair<InetAddress,Integer>, Socket>();

    private ConcurrentHashMap<Socket, Lock> socketLockMap = new ConcurrentHashMap<Socket, Lock>();

    public BackupMessageUtil() {
        workQueue = new LinkedBlockingQueue<Runnable>();
        executor = new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS, 1000, TimeUnit.MILLISECONDS, workQueue);
    }
    
    public BackupConfig getBackupConfig() {
        return bakcfg;
    }
    
    private void reallyQueueMessage(final Message msg) {
        if( msg.getState() == Message.State.NeedsProcessing ) {
            msg.setState(Message.State.Queued);
            executor.execute(
                    new Runnable() {
                        public void run() {
                            msg.setState(Message.State.Processing);

                            if( msg instanceof RetrieveDataMessage ) {
                                RetrieveDataMessage rdm = (RetrieveDataMessage) msg;
                                responseHandlers.put(
                                        rdm.getTransactionID(),
                                        rdm.getResponseHandler());
                            }
                            send(msg);
                            msg.setState(Message.State.Finished);
                        }
                    });
        }
    }

    protected void send(Message msg) {
        //TODO handle proxying somehow someday :-)
        Socket socket = null;
System.out.println("sending "+ msg.getTransactionID());
        try {
System.out.println("Attempting to talk to " + msg.getDestination().getAddress() + ":" + msg.getDestination().getPort());
            socket = acquireSocket(msg.getDestination().getAddress(), msg.getDestination().getPort());

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            msg.write(dos);
            dos.flush();

            //block until message is processed & response sent
            socket.getInputStream().read();
            
            destinationSuccesses.putIfAbsent(msg.getDestination(), new AtomicInteger(0));
            destinationSuccesses.get(msg.getDestination()).incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            destinationFailures.putIfAbsent(msg.getDestination(), new AtomicInteger(0));
            destinationFailures.get(msg.getDestination()).incrementAndGet();
            
            msg.setNumberOfTries(msg.getNumberOfTries() + 1);
            if( msg.getNumberOfTries() > MAX_TRIES ) {
                UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName());
                log.error("Not retrying, exceeded max tries: " + e.getMessage(), e);
                msg.setState(Message.State.Error);
            } else {
                UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName() + ", will retry", e);
                log.error("Failed to send message to " + msg.getDestination().getName() + ", will retry: " + e.getMessage(), e);
                
                msg.setState(Message.State.NeedsProcessing);
                reallyQueueMessage(msg);
            }
        } finally {
            if( socket != null ) {
                releaseSocket(socket);
System.out.println("Finished talking");
            }
            
            try {
                DataStore.instance().updateObject(msg);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
                //TODO user log (this exception isn't thrown now)
            }
        }
        System.out.println("sent "+ msg.getTransactionID());
    }

    private void releaseSocket(Socket socket) {
        Lock lock = socketLockMap.putIfAbsent(socket, new ReentrantLock());
        lock.unlock();
    }

    private Socket acquireSocket(InetAddress address, int port) throws IOException {
        Pair<InetAddress, Integer> key = new Pair<InetAddress, Integer>(address, port);
        Socket socket = socketMap.putIfAbsent(key, new Socket());
        if( socket == null ) {
            socket = socketMap.get(key);
        }

        socketLockMap.putIfAbsent(socket, new ReentrantLock());
        Lock lock = socketLockMap.get(socket);
        lock.lock();
        
        if( !socket.isConnected() ) {
            socket.connect(new InetSocketAddress(address, port));
        }

        return socket;
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
                                bakcfg.getLocalPort(),
                                id, 
                                makeReceiveDataHandler(id, continuation)));
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private Continuation makeErasureManifestHandler(
            final BinaryContinuation<String, byte[]> continuation,
            final LabelledData labelledData,
            final HashIdentifier erasureManifestId) {
        return new Continuation() {

            public void run() {
                try {
                    final ErasureManifest erasureManifest =
                            ErasureManifest.fromProto(
                                    Basic.ErasureManifest.parseFrom(
                                            DataStore.instance().getData(erasureManifestId)));
                    final List<Pair<HashIdentifier,RemoteNodeHandle>> retrievalData = erasureManifest.getRetrievalData();
                    final List<HashIdentifier> erasureIds = Pair.firstList(retrievalData);
                    
                    for(Pair<HashIdentifier, RemoteNodeHandle> retrievalDatum : retrievalData) {
                        retrieve(retrievalDatum.getSecond(), retrievalDatum.getFirst(), makeErasureHandler(continuation,
                                labelledData, erasureManifest,
                                erasureIds));
                    }
                } catch (InvalidProtocolBufferException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError("Failed to retrieve " + labelledData.getLabel(), e);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError("Failed to retrieve " + labelledData.getLabel(), e);
                } catch (UnknownHostException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError("Failed to retrieve " + labelledData.getLabel(), e);
                }
            }
        };
    }

    private Continuation makeErasureHandler(
            final BinaryContinuation<String, byte[]> continuation,
            final LabelledData labelledData,
            final ErasureManifest erasureManifest,
            final List<HashIdentifier> erasureIds) {
        return new Continuation() {

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
        };
    }

    private UnaryContinuation<byte[]> makeReceiveDataHandler(
            final HashIdentifier id, final Continuation continuation) {
        return new UnaryContinuation<byte[]>() {

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
                     };
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
        retrieve(storingNodes, id, makeLabelledDataHandler(storingNodes, id, continuation));
    }

    private Continuation makeLabelledDataHandler(
            final RemoteNodeHandle[] storingNodes, final HashIdentifier id,
            final BinaryContinuation<String, byte[]> continuation) {
        return new Continuation() {

            public void run() {
                try {
                    final LabelledData labelledData = LabelledData.fromProto(Basic.LabelledData.parseFrom(DataStore.instance().getData(id)));
                    final HashIdentifier erasureManifestId = labelledData.getPointingAt();
                    
                    log.info("Retrieved " + labelledData.getLabel());
                    
                    retrieve(storingNodes, erasureManifestId, makeErasureManifestHandler(continuation, labelledData,
                            erasureManifestId));
                } catch (InvalidProtocolBufferException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError("Failed to retrieve " + id, e);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError("Failed to retrieve " + id, e);
                }
            }
        };
    }
    
    public void retrieve(
            RemoteNodeHandle node,
            final HashIdentifier id,
            final Continuation continuation) throws SQLException {
        MessageUtil.instance().queueMessage(
                new RetrieveDataMessage(
                        node,
                        bakcfg.getLocalPort(),
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
                        })
                );
    }

    public void setBackupConfig(BackupConfig bakcfg) {
        this.bakcfg = bakcfg;
    }

    public void processMessage(Message msg, InetAddress inetAddress) throws SQLException {
        System.out.println("processing " + msg.getTransactionID());
        
        msg.setState(Message.State.Processing);

        if( msg instanceof VerifyMaybeSendDataMessage ) {
            VerifyMaybeSendDataMessage dataMessage = (VerifyMaybeSendDataMessage) msg;

            UnaryContinuation<byte[]> responseHandler =
                    responseHandlers.get(dataMessage.getTransactionID());

            //unsolicited data is assumed to be data we should store
            if( responseHandler == null ) {
                //TODO check that sender is in the friend node list
                DataStore.instance().storeData(dataMessage.getDataHashID(), dataMessage.getData(), dataMessage.getLease());
            } else {
                responseHandler.run(dataMessage.getData());
            }
            
            msg.setState(Message.State.Finished);
        } else if( msg instanceof RetrieveDataMessage) {
            RetrieveDataMessage retrieveMessage = (RetrieveDataMessage) msg;
            RemoteNodeHandle destination =
                    bakcfg.getFriend(
                            inetAddress,
                            retrieveMessage.getOriginNodePort());
            HashIdentifier hashIDOfDataToRetrieve = retrieveMessage.getHashIDOfDataToRetrieve();
            queueMessage(
                    new VerifyMaybeSendDataMessage(
                            destination,
                            retrieveMessage.getTransactionID(),
                            bakcfg.getLocalPort(),
                            hashIDOfDataToRetrieve,
                            DataStore.instance().getData(hashIDOfDataToRetrieve),
                            null));

            msg.setState(Message.State.Finished);
        } else {
            msg.setState(Message.State.Error);
            log.error("Unexpected message type; message: " + msg + " from inetAddress " + inetAddress);
        }

        System.out.println("processed " + msg.getTransactionID());
    }
}
