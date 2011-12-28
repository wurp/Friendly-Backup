package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.format.high.BackupManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.identity.PrivateIdentity;
import com.geekcommune.util.BinaryContinuation;
import com.geekcommune.util.FileUtil;
import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Reconstitutes all the files from a backup.
 * Restore is a multi-step process.  See the main() method for a simple example of restore.
 *   -     
 * @see com.geekcommune.friendlybackup.main.Action
 * @see com.geekcommune.friendlybackup.main.Restore
 * 
 * @author bobbym
 *
 */
public class Restore extends Action {
    private static final Logger log = Logger.getLogger(Restore.class);
	
	private ProgressTracker progressTracker;

    public Restore() throws IOException {
        super();
    }

    /**
	 * Initiates a restore.  The restore will automatically continue in another thread - start returns immediately
	 * after the backup manifest is retrieved.
     * @throws IOException 
	 */
	public void start(final PrivateIdentity authenticatedOwner) throws IOException {
	    final BackupConfig bakcfg = App.getBackupConfig();
	    
        final UserLog userlog = UserLog.instance();

        //retrieve my latest backup manifest label
        final HashIdentifier backupLabelId = LabelledData.getHashID(
                authenticatedOwner.getPublicIdentity().getHandle(),
                bakcfg.getBackupStreamName());

        progressTracker = new ProgressTracker(105);
        
        //retrieve my latest backup erasure manifest
        //TODO we only need one, but we'll just send out requests to everyone for now
        BackupMessageUtil.instance().retrieveLabelledData(
                bakcfg.getStoringNodes(),
                backupLabelId,
                restoreBackupManifestResponseHandler(bakcfg, userlog));//end retrieveLabelledData(backupLabelId)
	}//end start()

    private BinaryContinuation<String, byte[]> restoreBackupManifestResponseHandler(
            final BackupConfig bakcfg, final UserLog userlog) {
        return new BinaryContinuation<String,byte[]>() {
            public void run(String label, byte[] backupManifestContents) {
                try {
                    BackupManifest bakman = BackupManifest.fromProto(
                            Basic.BackupManifest.parseFrom(backupManifestContents));

                    progressTracker.rebase(bakman.getFileLabelIDs().size() * 2);
                    
                    //loop over each label id in the backup manifest
                    for(HashIdentifier fileLabelId : bakman.getFileLabelIDs()) {
                        //  retrieve the label
                        BackupMessageUtil.instance().retrieveLabelledData(bakcfg.getStoringNodes(), fileLabelId, restoreFileContentsHandler(bakcfg, userlog));//end retrieveLabelledData(fileLabelId)
                    }//end for(fileLabelId)
                } catch (InvalidProtocolBufferException e1) {
                    log.error(e1.getMessage(), e1);
                } //end try/catch
            }//end run()
        };
    }

    private BinaryContinuation<String, byte[]> restoreFileContentsHandler(
            final BackupConfig bakcfg, final UserLog userlog) {
        return new BinaryContinuation<String, byte[]>() {

            public void run(String label, byte[] t) {
                //Danger, Will Robinson!  Overwriting the file!  TODO
                try {
                    File file = new File(bakcfg.getRestorePath(label));

                    if( file.exists() ) {
                        userlog.logError(file + " already exists; not overwriting");
                        progressTracker.changeMessage("Not writing " + label, 2);
                    } else {
                        log.info("Saving " + label);

                        progressTracker.changeMessage("Writing " + label, 1);

                        FileUtil.instance().createPathAndWriteFile(file, t);
                        
                        progressTracker.changeMessage("Wrote " + label, 1);
                    }//end if(file.exists())
                    
                    if( progressTracker.stepsRemaining() == 0 ) {
                        progressTracker.setFinished(true);
                    }
                } catch (IOException e1) {
                    log.error(e1.getMessage(), e1);
                }
            }//end run()
        };
    }

    public ProgressTracker getProgressTracker() {
		return progressTracker;
	}

	public void blockUntilDone() throws InterruptedException {
		 ProgressTracker progressTracker = getProgressTracker();
		 while( !progressTracker.isFinished() && !progressTracker.isFailed() ) {
		     Thread.sleep(1000);
		 }
	}
	
	public void doRestore(String password) throws InterruptedException, IOException, ClassNotFoundException, SQLException {
        start(App.getBackupConfig().getAuthenticatedOwner(password));
        ProgressTracker progressTracker = getProgressTracker();
        while( !progressTracker.isFinished() && !progressTracker.isFailed() ) {
            System.out.println(progressTracker.getStatusMessage());
            Thread.sleep(1000);
        }
	}

	public static void main(String[] args) throws Exception {
		Restore restore = new Restore();
		String password = args[0];
		restore.doRestore(password);
	}
}
