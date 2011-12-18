package com.geekcommune.friendlybackup.builder;

import java.io.File;
import java.io.IOException;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendErasureMessage;
import com.geekcommune.friendlybackup.erasurefinder.BytesErasureFinder;
import com.geekcommune.friendlybackup.erasurefinder.ErasureFinder;
import com.geekcommune.friendlybackup.erasurefinder.ErasureUtil;
import com.geekcommune.friendlybackup.erasurefinder.FileErasureFinder;
import com.geekcommune.friendlybackup.erasurefinder.FileUtil;
import com.geekcommune.friendlybackup.format.low.BufferData;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
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
	private static ErasureManifestBuilder instance = new ErasureManifestBuilder();

    public ErasureManifest buildFromFile(RemoteNodeHandle[] storingNodes, File f, final int erasuresNeeded, final int totalErasures) throws IOException {
		byte[] data = FileUtil.instance().getFileContents(f);
		ErasureFinder erasureFinder = new FileErasureFinder(f, erasuresNeeded, totalErasures);
		
		return build(data, erasureFinder, storingNodes, erasuresNeeded,
				totalErasures);
	}

	private ErasureManifest build(byte[] data, ErasureFinder erasureFinder,
			RemoteNodeHandle[] storingNodes, final int erasuresNeeded,
			final int totalErasures) {
		ErasureManifest manifest = new ErasureManifest();

		//break the file down into totalErasures chunks, in such a way that we can reconstitute it from any erasuresNeeded chunks
		Buffer[] erasures = ErasureUtil.encode(data, erasuresNeeded, totalErasures);
		
		int idx = 0;
		for(Buffer erasureBuffer : erasures) {
		    BufferData erasure = new BufferData(erasureBuffer, idx);
			HashIdentifier erasureId = erasure.getHashID();
			
			//store the erasure on its correct node
			RemoteNodeHandle storingNode = calculateStoringNode(storingNodes, idx);
			MessageUtil.instance().queueMessage(storingNode, new VerifyMaybeSendErasureMessage(erasureId, erasureFinder, idx));

			//put the erasure in the manifest
			manifest.add(erasureId, storingNode);
			
			++idx;
		}

		//for now, store the manifest on all storing nodes
		for(RemoteNodeHandle node : storingNodes) {
			MessageUtil.instance().queueMessage(node, new VerifyMaybeSendDataMessage(manifest.getHashID(), manifest.toProto().toByteArray()));
		}
		
		manifest.setContentSize(data.length);
		manifest.setErasuresNeeded(erasuresNeeded);
		manifest.setTotalErasures(totalErasures);
		
		return manifest;
	}

	private RemoteNodeHandle calculateStoringNode(
			RemoteNodeHandle[] storingNodes, int idx) {
		// TODO figure out a way to not leave the last node(s) underused.  Add hash of erasure manifest before doing modulo?
		return storingNodes[idx % storingNodes.length];
	}

	public ErasureManifest buildFromBytes(RemoteNodeHandle[] storingNodes, byte[] data, int erasuresNeeded, int totalErasures) {
		ErasureFinder erasureFinder = new BytesErasureFinder(data, erasuresNeeded, totalErasures);
		
		return build(data, erasureFinder, storingNodes, erasuresNeeded,
				totalErasures);
	}

	public static ErasureManifestBuilder instance() {
		return instance ;
	}
}
