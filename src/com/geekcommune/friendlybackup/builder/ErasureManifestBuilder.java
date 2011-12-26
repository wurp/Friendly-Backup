package com.geekcommune.friendlybackup.builder;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendErasureMessage;
import com.geekcommune.friendlybackup.erasure.BytesErasureFinder;
import com.geekcommune.friendlybackup.erasure.ErasureFinder;
import com.geekcommune.friendlybackup.erasure.ErasureUtil;
import com.geekcommune.friendlybackup.erasure.FileErasureFinder;
import com.geekcommune.friendlybackup.format.low.Erasure;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.identity.PrivateIdentity;
import com.geekcommune.util.FileUtil;
import com.onionnetworks.util.Buffer;

/**
 * This class is in main because it uses a lot of other functionality.
 * It reads files, creates the list of erasures for a dataset, queues messages up
 * to be sent, and creates the erasure manifest.
 * 
 * @author bobbym
 *
 */
public class ErasureManifestBuilder {
    private static final Logger log = Logger.getLogger(ErasureManifestBuilder.class);

    private static ErasureManifestBuilder instance = new ErasureManifestBuilder();

    public ErasureManifest buildFromFile(
            RemoteNodeHandle[] storingNodes,
            File f,
            final int erasuresNeeded,
            final int totalErasures,
            Date expiryDate,
            PrivateIdentity owner) throws IOException {
		byte[] data = FileUtil.instance().getFileContents(f);
		ErasureFinder erasureFinder = new FileErasureFinder(f, erasuresNeeded, totalErasures);
		
		return build(
		        data,
		        erasureFinder,
		        storingNodes,
		        erasuresNeeded,
				totalErasures,
				expiryDate,
				owner);
	}

	private ErasureManifest build(
	        byte[] data,
	        ErasureFinder erasureFinder,
			RemoteNodeHandle[] storingNodes,
			final int erasuresNeeded,
			final int totalErasures,
			final Date expiryDate,
			final PrivateIdentity owner) {
		ErasureManifest manifest = new ErasureManifest();

		//break the file down into totalErasures chunks, in such a way that we can reconstitute it from any erasuresNeeded chunks
		Buffer[] erasures = ErasureUtil.encode(data, erasuresNeeded, totalErasures);
		
		int idx = 0;
		for(Buffer erasureBuffer : erasures) {
		    Erasure erasure = new Erasure(erasureBuffer, idx);
			HashIdentifier erasureId = erasure.getHashID();
			
			//store the erasure on its correct node
			RemoteNodeHandle storingNode = calculateStoringNode(storingNodes, idx);
			try {
                MessageUtil.instance().queueMessage(
                        new VerifyMaybeSendErasureMessage(
                                storingNode,
                                erasureId,
                                erasureFinder,
                                idx,
                                owner.makeLease(erasureId, expiryDate)));
            } catch (SQLException e) {
                //TODO user message
                log.error(e.getMessage(), e);
            }

			//put the erasure in the manifest
			manifest.add(erasureId, storingNode);
			
			++idx;
		}
        
        manifest.setContentSize(data.length);
        manifest.setErasuresNeeded(erasuresNeeded);
        manifest.setTotalErasures(totalErasures);

		//for now, store the manifest on all storing nodes
		for(RemoteNodeHandle node : storingNodes) {
			try {
                MessageUtil.instance().queueMessage(new VerifyMaybeSendDataMessage(
                        node,
                        manifest.getHashID(),
                        manifest.toProto().toByteArray(),
                        owner.makeLease(manifest.getHashID(), expiryDate)));
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
		}
		
		return manifest;
	}

	private RemoteNodeHandle calculateStoringNode(
			RemoteNodeHandle[] storingNodes, int idx) {
		// TODO figure out a way to not leave the last node(s) underused.  Add hash of erasure manifest before doing modulo?
		return storingNodes[idx % storingNodes.length];
	}

	public ErasureManifest buildFromBytes(RemoteNodeHandle[] storingNodes, byte[] data, int erasuresNeeded, int totalErasures, Date expiryDate, PrivateIdentity owner) {
		ErasureFinder erasureFinder = new BytesErasureFinder(data, erasuresNeeded, totalErasures);
		
		return build(
		        data,
		        erasureFinder,
		        storingNodes,
		        erasuresNeeded,
				totalErasures,
                expiryDate,
                owner);
	}

	public static ErasureManifestBuilder instance() {
		return instance ;
	}
}
