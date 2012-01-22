package com.geekcommune.friendlybackup.communication;


import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
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
    public static final Logger log = Logger.getLogger(BackupMessageUtil.class);

    private static BackupMessageUtil instance;

    public static BackupMessageUtil instance() {
        return instance;
    }

    public static void setInstance(BackupMessageUtil instance) {
        BackupMessageUtil.instance = instance;
    }

    public BackupConfig bakcfg;
    public BackupMessageUtil() {
        initMessageHandlers();
    }
    
    private void initMessageHandlers() {
    	addMessageHandler(new MessageHandler() {
			@Override
			public boolean handleMessage(Message msg, InetAddress address, boolean responseHandled) throws FriendlyBackupException {
				boolean handled = false;
		        if( !responseHandled && msg instanceof VerifyMaybeSendDataMessage ) {
		        	handled = true;
		            VerifyMaybeSendDataMessage dataMessage = (VerifyMaybeSendDataMessage) msg;

	                //TODO check that sender (and lease owner) is in the friend node list
	                DataStore.instance().storeData(dataMessage.getDataHashID(), dataMessage.getData(), dataMessage.getLease());
		            
		            msg.setState(Message.State.Finished);
		        }
		        
				return handled;
			}
		});
		
    	addMessageHandler(new MessageHandler() {
			@Override
			public boolean handleMessage(Message msg, InetAddress address, boolean responseHandled) throws FriendlyBackupException {
				boolean handled = false;
				if( msg instanceof RetrieveDataMessage) {
					handled = true;
		            RetrieveDataMessage retrieveMessage = (RetrieveDataMessage) msg;
		            RemoteNodeHandle destination =
		                    bakcfg.getFriend(
		                    		address,
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
		        }
				
				return handled;
			}
    	});
	}

	public BackupConfig getBackupConfig() {
        return bakcfg;
    }
    
    /**
     * Pull in a piece of data, retrieving from any/all of storingNodes if necessary.
     * @param storingNodes 
     * @param id
     * @param continuation
     */
    public void retrieve(RemoteNodeHandle[] storingNodes, final HashIdentifier id, final Continuation continuation) {
        ResponseManager requestManager = new ResponseManager();
        
        UnaryContinuation<Message> handler = makeReceiveDataHandler(id, continuation, requestManager);
        
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
                } catch (FriendlyBackupException e) {
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

    private UnaryContinuation<Message> makeReceiveDataHandler(
            final HashIdentifier id,
            final Continuation continuation,
            final ResponseManager requestManager) {
        return new UnaryContinuation<Message>() {

        public void run(final Message msg) {
            requestManager.doOnce(new Continuation() {
                
                public void run() {
                    try {
                    	VerifyMaybeSendDataMessage vmsdm = (VerifyMaybeSendDataMessage) msg;
                    	byte[] data = vmsdm.getData();
                        DataStore.instance().storeData(id, data, new Lease(DateUtil.oneHourHence(), bakcfg.getOwner().getHandle(), Signature.INTERNAL_SELF_SIGNED, id));
                        continuation.run();
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

	@Override
	protected int getLocalPort() {
		return bakcfg.getLocalPort();
	}
}
