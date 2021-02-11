package com.geekcommune.friendlybackup.builder;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.communication.ProgressWhenCompleteListener;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendErasureMessage;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.erasure.BytesErasureFinder;
import com.geekcommune.friendlybackup.erasure.ErasureFinder;
import com.geekcommune.friendlybackup.erasure.ErasureUtil;
import com.geekcommune.friendlybackup.erasure.FileErasureFinder;
import com.geekcommune.friendlybackup.format.low.Erasure;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.main.ProgressTracker;
import com.geekcommune.identity.SecretIdentity;
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
    //private static final Logger log = Logger.getLogger(ErasureManifestBuilder.class);

    private static ErasureManifestBuilder instance = new ErasureManifestBuilder();

    public ErasureManifest buildFromFile(
            RemoteNodeHandle[] storingNodes,
            File f,
            BackupConfig bakcfg,
            Date expiryDate,
            SecretIdentity owner,
            ProgressTracker progressTracker) throws IOException, FriendlyBackupException {
        int erasuresNeeded = bakcfg.getErasuresNeeded();
        int totalErasures = bakcfg.getTotalErasures();
        
        ErasureFinder erasureFinder =
                new FileErasureFinder(
                        f,
                        owner,
                        erasuresNeeded,
                        totalErasures);
        
        return build(
                FileUtil.instance().getFileContents(f),
                erasureFinder,
                storingNodes,
                bakcfg.getLocalPort(),
                erasuresNeeded,
                totalErasures,
                expiryDate,
                owner,
                progressTracker);
    }

    private ErasureManifest build(
            byte[] data,
            ErasureFinder erasureFinder,
            RemoteNodeHandle[] storingNodes,
            final int localPort,
            final int erasuresNeeded,
            final int totalErasures,
            final Date expiryDate,
            final SecretIdentity owner,
            ProgressTracker progressTracker) throws FriendlyBackupException {
        return buildFromEncrypted(owner.encryptConsistently(data), erasureFinder, storingNodes, localPort, erasuresNeeded, totalErasures, expiryDate, owner, progressTracker);
    }

    private ErasureManifest buildFromEncrypted(
            byte[] data,
            ErasureFinder erasureFinder,
            RemoteNodeHandle[] storingNodes,
            final int localPort,
            final int erasuresNeeded,
            final int totalErasures,
            final Date expiryDate,
            final SecretIdentity owner,
            ProgressTracker progressTracker) throws FriendlyBackupException {
        progressTracker.rebase(totalErasures + storingNodes.length);

        ErasureManifest manifest = new ErasureManifest();

        //break the file down into totalErasures chunks, in such a way that we
        //can reconstitute it from any erasuresNeeded chunks
        Buffer[] erasures = ErasureUtil.encode(data, erasuresNeeded, totalErasures);

        int idx = 0;
        for(Buffer erasureBuffer : erasures) {
            Erasure erasure = new Erasure(erasureBuffer, idx);
            HashIdentifier erasureId = erasure.getHashID();

            //store the erasure on its correct node
            RemoteNodeHandle storingNode = calculateStoringNode(storingNodes, idx);
            VerifyMaybeSendErasureMessage msg = new VerifyMaybeSendErasureMessage(
                    storingNode,
                    localPort,
                    erasureId,
                    erasureFinder,
                    idx,
                    new Lease(expiryDate, owner, false, erasureId));
            msg.addStateListener(new ProgressWhenCompleteListener(progressTracker, 1));
            BackupMessageUtil.instance().queueMessage(
                    msg);

            //put the erasure in the manifest
            manifest.add(erasureId, storingNode);
            
            ++idx;
        }

        manifest.setContentSize(data.length);
        manifest.setErasuresNeeded(erasuresNeeded);
        manifest.setTotalErasures(totalErasures);

        //for now, store the manifest on all storing nodes
        for(RemoteNodeHandle node : storingNodes) {
            VerifyMaybeSendDataMessage msg = new VerifyMaybeSendDataMessage(
                    node,
                    localPort,
                    manifest.getHashID(),
                    manifest.toProto().toByteArray(),
                    new Lease(expiryDate, owner, false, manifest.getHashID()));
            msg.addStateListener(new ProgressWhenCompleteListener(progressTracker, 1));
            BackupMessageUtil.instance().queueMessage(msg);
        }

        return manifest;
    }

    private RemoteNodeHandle calculateStoringNode(
            RemoteNodeHandle[] storingNodes, int idx) {
        // TODO figure out a way to not leave the last node(s) underused.  Add hash of erasure manifest before doing modulo?
        return storingNodes[idx % storingNodes.length];
    }

    public ErasureManifest buildFromBytes(
            RemoteNodeHandle[] storingNodes,
            byte[] data,
            BackupConfig bakcfg,
            Date expiryDate,
            SecretIdentity owner,
            ProgressTracker progressTracker) throws FriendlyBackupException {
        int erasuresNeeded = bakcfg.getErasuresNeeded();
        int totalErasures = bakcfg.getTotalErasures();

        data = owner.encryptConsistently(data);
        
        ErasureFinder erasureFinder =
                new BytesErasureFinder(
                        data,
                        erasuresNeeded,
                        totalErasures);

        return buildFromEncrypted(
                data,
                erasureFinder,
                storingNodes,
                bakcfg.getLocalPort(),
                erasuresNeeded,
                totalErasures,
                expiryDate,
                owner,
                progressTracker);
    }

    public static ErasureManifestBuilder instance() {
        return instance ;
    }
}
