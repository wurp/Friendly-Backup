package com.geekcommune.friendlybackup.communication;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
import com.geekcommune.communication.message.AbstractMessage;
import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.FriendlyBackupException;
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
    private Executor sendExecutor;
    private ConcurrentHashMap<RemoteNodeHandle, AtomicInteger> destinationFailures = new ConcurrentHashMap<RemoteNodeHandle, AtomicInteger>();
    private ConcurrentHashMap<RemoteNodeHandle, AtomicInteger> destinationSuccesses = new ConcurrentHashMap<RemoteNodeHandle, AtomicInteger>();

    /**
     * Map from transaction id to the continuation for handling the data requested in that xaction
     */
    private ConcurrentHashMap<Integer, UnaryContinuation<byte[]>> responseHandlers = new ConcurrentHashMap<Integer, UnaryContinuation<byte[]>>();

    private ConcurrentHashMap<Pair<InetAddress, Integer>, Socket> socketMap = new ConcurrentHashMap<Pair<InetAddress,Integer>, Socket>();

    private ConcurrentHashMap<Socket, Lock> socketLockMap = new ConcurrentHashMap<Socket, Lock>();

    private Thread listenThread;

    private ThreadPoolExecutor listenExecutor;


    public BackupMessageUtil() {
        LinkedBlockingQueue<Runnable> sendWorkQueue = new LinkedBlockingQueue<Runnable>();
        sendExecutor = new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS, 1000, TimeUnit.MILLISECONDS, sendWorkQueue);
    }
    
    public BackupConfig getBackupConfig() {
        return bakcfg;
    }
    
    @Override
    public void queueMessage(final Message msg) {
        super.queueMessage(msg);
        if( msg.getState() == Message.State.NeedsProcessing ) {
            msg.setState(Message.State.Queued);
            sendExecutor.execute(
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
                        }
                    });
        }
    }

    protected void send(Message msg) {
        //TODO handle proxying somehow someday :-)
        Message.State state = Message.State.Error;
        
        Socket socket = null;
        log.debug("sending "+ msg.getTransactionID());
        try {
            log.debug("Attempting to talk to " + msg.getDestination().getAddress() + ":" + msg.getDestination().getPort());
            socket = acquireSocket(msg.getDestination().getAddress(), msg.getDestination().getPort());

            justSend(msg, socket);
            state = Message.State.Finished;
            
            destinationSuccesses.putIfAbsent(msg.getDestination(), new AtomicInteger(0));
            destinationSuccesses.get(msg.getDestination()).incrementAndGet();
        } catch (IOException e) {
            removeSocket(socket);
            
            destinationFailures.putIfAbsent(msg.getDestination(), new AtomicInteger(0));
            destinationFailures.get(msg.getDestination()).incrementAndGet();
            
            msg.setNumberOfTries(msg.getNumberOfTries() + 1);
            if( msg.getNumberOfTries() > MAX_TRIES ) {
                UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName(), e);
                log.error("Not retrying, exceeded max tries: " + e.getMessage(), e);
            } else {
                UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName() + ", will retry", e);
                log.error("Failed to send message to " + msg.getDestination().getName() + ", will retry: " + e.getMessage(), e);
                
                state = Message.State.NeedsProcessing;
                queueMessage(msg);
            }
        } catch (FriendlyBackupException e) {
            UserLog.instance().logError("Failed to send message to " + msg.getDestination().getName(), e);
        } finally {
            msg.setState(state);
            
            if( socket != null ) {
                releaseSocket(socket);
                log.debug("Finished talking");
            }
        }
        log.debug("sent "+ msg.getTransactionID());
    }

    private void justSend(Message msg, Socket socket) throws IOException, FriendlyBackupException {
        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        msg.write(dos);
        dos.flush();

        //block until message is processed & response sent
        socket.getInputStream().read();
    }

    private void releaseSocket(Socket socket) {
        Lock lock = socketLockMap.get(socket);
        
        if( lock != null ) {
            lock.unlock();
        }
    }

    private void removeSocket(Socket socket) {
        if( socket != null ) {
            Lock lock = socketLockMap.get(socket);
            
            if( lock != null ) {
                lock.unlock();
                socketLockMap.remove(socket);
            }
            
            Pair<InetAddress, Integer> key = new Pair<InetAddress, Integer>(socket.getInetAddress(), socket.getPort());
            socketMap.remove(key);
        }
    }

    private Socket acquireSocket(InetAddress address, int port) throws IOException {
        Pair<InetAddress, Integer> key = new Pair<InetAddress, Integer>(address, port);
        Socket socket = socketMap.putIfAbsent(key, new Socket());
        if( socket == null ) {
            socket = socketMap.get(key);
        }

        //TODO not entirely thread safe
        socketLockMap.putIfAbsent(socket, new ReentrantLock());
        Lock lock = socketLockMap.get(socket);
        lock.lock();
        
        if( !socket.isConnected() ) {
            socket.connect(new InetSocketAddress(address, port));
        }

        return socket;
    }

    /**
     * Pull in a piece of data, retrieving from any/all of storingNodes if necessary.
     * @param storingNodes 
     * @param id
     * @param continuation
     */
    public void retrieve(RemoteNodeHandle[] storingNodes, final HashIdentifier id, final Continuation continuation) {
        ResponseManager requestManager = new ResponseManager();
        
        UnaryContinuation<byte[]> handler = makeReceiveDataHandler(id, continuation, requestManager);
        
        for(RemoteNodeHandle storingNode : storingNodes) {
            RetrieveDataMessage msg = new RetrieveDataMessage(
                    storingNode,
                    bakcfg.getLocalPort(),
                    id,
                    handler);
            
            queueMessage(msg);
        }
    }

    private Continuation makeErasureManifestHandler(
            final BinaryContinuation<String, byte[]> continuation,
            final LabelledData labelledData,
            final HashIdentifier erasureManifestId) {
        return new Continuation() {

            public void run() {
                String label = "<unable to retrieve label>";
                
                try {
                    label = labelledData.getLabel(bakcfg.getAuthenticatedOwner());
                } catch (FriendlyBackupException e) {
                    UserLog.instance().logError(label, e);
                }

                final String exceptionMessage = "Failed to retrieve " + label;
                
                try {
                    final ErasureManifest erasureManifest =
                            ErasureManifest.fromProto(
                                    Basic.ErasureManifest.parseFrom(
                                            DataStore.instance().getData(erasureManifestId)));
                    final List<Pair<HashIdentifier,RemoteNodeHandle>> retrievalData = erasureManifest.getRetrievalData();
                    
                    Continuation erasureHandler = makeErasureHandler(
                            continuation,
                            labelledData,
                            erasureManifest,
                            new ResponseManager(erasureManifest.getErasuresNeeded()));

                    for(Pair<HashIdentifier, RemoteNodeHandle> retrievalDatum : retrievalData) {
                        retrieve(
                                retrievalDatum.getSecond(),
                                retrievalDatum.getFirst(),
                                erasureHandler);
                    }
                    //TODO delete erasureManifestId data from datastore
                } catch (InvalidProtocolBufferException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError(exceptionMessage, e);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError(exceptionMessage, e);
                } catch (UnknownHostException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError(exceptionMessage, e);
                }
            }
        };
    }

    private Continuation makeErasureHandler(
            final BinaryContinuation<String, byte[]> continuation,
            final LabelledData labelledData,
            final ErasureManifest erasureManifest,
            final ResponseManager responseManager) {
        return new Continuation() {

            public void run() {
                responseManager.doOnce(new Continuation() {
                    
                    public void run() {
                        String label = "<unable to retrieve label>";
                        
                        try {
                            label = labelledData.getLabel(bakcfg.getAuthenticatedOwner());
                        } catch (FriendlyBackupException e) {
                            UserLog.instance().logError(label, e);
                        }

                        final String exceptionMessage = "Failed to retrieve " + label;
                        
                        try {
                            
                            List<HashIdentifier> erasureIds = Pair.firstList(erasureManifest.getRetrievalData());
                            
                            //check if enough of the blocks have come in to reconstitute the data
                            List<byte[]> dataList = DataStore.instance().getDataList(erasureIds);

                            if( dataList.size() >= erasureManifest.getErasuresNeeded() ) {
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
                                
                                fullContents = bakcfg.getAuthenticatedOwner().decrypt(fullContents);
                                
                                log.info("Rebuilt contents of " + label);

                                continuation.run(label, fullContents);
                                
                                //TODO clean dataList out of datastore
                                //TODO somehow switch responseManager behavior to delete new ids that come in from the DataStore
                            } else {
                                log.error("not enough blocks returned to rebuild erasure - how did we get here?");
                            }
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            UserLog.instance().logError(exceptionMessage, e);
                        }
                    }
                });
            }
        };
    }

    private UnaryContinuation<byte[]> makeReceiveDataHandler(
            final HashIdentifier id,
            final Continuation continuation,
            final ResponseManager requestManager) {
        return new UnaryContinuation<byte[]>() {

        public void run(final byte[] data) {
            requestManager.doOnce(new Continuation() {
                
                public void run() {
                    try {
                        DataStore.instance().storeData(id, data, new Lease(DateUtil.oneHourHence(), bakcfg.getOwner().getHandle(), Signature.INTERNAL_SELF_SIGNED, id));
                        continuation.run();
                    } catch (SQLException e) {
                        log.error(e.getMessage(), e);
                        UserLog.instance().logError("Failed to retrieve " + id, e);
                    } catch (FriendlyBackupException e) {
                        log.error(e.getMessage(), e);
                        UserLog.instance().logError("Failed to retrieve " + id, e);
                    }
                }
            });
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
                String exceptionMessage = "Failed to retrieve ";
                try {
                    Basic.LabelledData proto =
                            Basic.LabelledData.parseFrom(DataStore.instance().getData(id));
                    final LabelledData labelledData =
                            LabelledData.fromProto(proto);

                    String label = labelledData.getLabel(bakcfg.getAuthenticatedOwner());
                    if( !labelledData.verifySignature(bakcfg.getPublicKeyRingCollection()) ) {
                        throw new FriendlyBackupException("Could not validate signature on " +
                                label);
                    }
                    
                    final HashIdentifier erasureManifestId = labelledData.getPointingAt();
                    log.debug("Retrieving " + label);
                    
                    retrieve(storingNodes, erasureManifestId, makeErasureManifestHandler(continuation, labelledData,
                            erasureManifestId));
                    //TODO delete 'id' data from datastore
                } catch (InvalidProtocolBufferException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError(exceptionMessage + id, e);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                    UserLog.instance().logError(exceptionMessage + id, e);
                } catch (FriendlyBackupException e) {
                    UserLog.instance().logError(exceptionMessage + id, e);
                }
            }
        };
    }
    
    public void retrieve(
            RemoteNodeHandle node,
            final HashIdentifier id,
            final Continuation continuation) throws SQLException {
        RetrieveDataMessage msg = new RetrieveDataMessage(
                        node,
                        bakcfg.getLocalPort(),
                        id,
                        makeReceiveDataHandler(id, continuation, new ResponseManager())
                );
        queueMessage(msg);
    }

    public void setBackupConfig(BackupConfig bakcfg) {
        this.bakcfg = bakcfg;
    }

    public void startListenThread() {
        if( listenThread != null ) {
            throw new RuntimeException("Listen thread already started");
        }

        LinkedBlockingQueue<Runnable> listenWorkQueue = new LinkedBlockingQueue<Runnable>();
        listenExecutor = new ThreadPoolExecutor(NUM_THREADS, NUM_THREADS, 1000, TimeUnit.MILLISECONDS, listenWorkQueue);

        listenThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ServerSocket serversocket = null;
                    serversocket = new ServerSocket(bakcfg.getLocalPort());
                    log.debug("server socket listening on " + bakcfg.getLocalPort());
                    do {
                        try {
                            Socket socket = serversocket.accept();
                            log.debug("Server socket open");
                            listenExecutor.execute(makeHandleAllMessagesOnSocketRunnable(socket));
                        } catch(Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    } while (true);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Couldn't start listening for unsolicited messages: " + e.getMessage(), e);
                }
            }
        });

        listenThread.start();
    }

    private Runnable makeHandleAllMessagesOnSocketRunnable(final Socket socket) {
        return new Runnable() {
            public void run() {
                try {
                    while(socket.isConnected() && !socket.isInputShutdown()) {
                        DataInputStream dis = new DataInputStream(socket.getInputStream());
                        final Message msg = AbstractMessage.parseMessage(dis);
                        final InetAddress address = socket.getInetAddress();
                        msg.setState(Message.State.NeedsProcessing);
                        makeProcessMessageRunnable(msg, address).run();
                        socket.getOutputStream().write(1);
                        socket.getOutputStream().flush();
                    }
                } catch (IOException e) {
                    log.error("Error talking to " + socket + ", " + e.getMessage(), e);
                } catch (FriendlyBackupException e) {
                    log.error("Error talking to " + socket + ", " + e.getMessage(), e);
                } finally {
                    try {
                        log.debug("Server socket finished");
                        socket.close();
                    } catch( Exception e ) {
                        log.error("Error closing socket to " + socket + ", " + e.getMessage(), e);
                    }
                }
            }
        };
    }

    private Runnable makeProcessMessageRunnable(final Message msg,
            final InetAddress address) {
        return new Runnable() {
            public void run() {
                try {
                    processMessage(msg, address);
                } catch (SQLException e) {
                    log.error("Error talking processing " + msg + ": " + e.getMessage(), e);
                } catch (FriendlyBackupException e) {
                    log.error("Error talking processing " + msg + ": " + e.getMessage(), e);
                }
            }
        };
    }

    public void processMessage(Message msg, InetAddress inetAddress) throws SQLException, FriendlyBackupException {
        log.debug("processing " + msg.getTransactionID());
        //TODO reject message if we've already processed its transaction id
        msg.setState(Message.State.Processing);

        if( msg instanceof VerifyMaybeSendDataMessage ) {
            VerifyMaybeSendDataMessage dataMessage = (VerifyMaybeSendDataMessage) msg;

            UnaryContinuation<byte[]> responseHandler =
                    responseHandlers.get(dataMessage.getTransactionID());

            //unsolicited data is assumed to be data we should store
            if( responseHandler == null ) {
                //TODO check that sender (and lease owner) is in the friend node list
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
                            DataStore.instance().getLeases(hashIDOfDataToRetrieve).get(0)));

            msg.setState(Message.State.Finished);
        } else {
            msg.setState(Message.State.Error);
            log.error("Unexpected message type; message: " + msg + " from inetAddress " + inetAddress);
        }

        log.debug("processed " + msg.getTransactionID());
    }
}
