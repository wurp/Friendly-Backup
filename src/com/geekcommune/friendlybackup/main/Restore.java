package com.geekcommune.friendlybackup.main;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.datastore.DataStore;
import com.geekcommune.friendlybackup.format.high.BackupManifest;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.identity.PrivateIdentity;
import com.geekcommune.util.BinaryContinuation;
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

    /**
	 * Initiates a restore.  The restore will automatically continue in another thread - start returns immediately
	 * after the backup manifest is retrieved.
     * @throws IOException 
	 */
	public void start(final PrivateIdentity authenticatedOwner) throws IOException {
	    final BackupConfig bakcfg = getBackupConfig();
	    
        final UserLog userlog = UserLog.instance();

        //retrieve my latest backup manifest label
        final HashIdentifier backupLabelId = LabelledData.getHashID(
                authenticatedOwner.getPublicIdentity().getHandle(),
                bakcfg.getBackupStreamName());

        progressTracker = new ProgressTracker(105);
        
        //retrieve my latest backup erasure manifest
        //TODO we only need one, but we'll just send out requests to everyone for now
        DataStore.instance().retrieveLabelledData(bakcfg.getStoringNodes(), backupLabelId, new BinaryContinuation<String,byte[]>() {
            public void run(String label, byte[] backupManifestContents) {
                try {
                    BackupManifest bakman = BackupManifest.fromProto(
                            Basic.BackupManifest.parseFrom(backupManifestContents));

                    progressTracker.rebase(bakman.getFileLabelIDs().size() * 2);
                    
                    //loop over each label id in the backup manifest
                    for(HashIdentifier fileLabelId : bakman.getFileLabelIDs()) {
                        //  retrieve the label
                        DataStore.instance().retrieveLabelledData(bakcfg.getStoringNodes(), fileLabelId, new BinaryContinuation<String, byte[]>() {

                            public void run(String label, byte[] t) {
                                //Danger, Will Robinson!  Overwriting the file!  TODO
                                File file = new File(bakcfg.parseFullFilePath(label)[1]);

                                if( file.exists() ) {
                                    userlog.logError(file + " already exists; not overwriting");
                                    progressTracker.changeMessage("Not writing " + label, 2);
                                } else {
                                    log.info("Saving " + label);

                                    progressTracker.changeMessage("Writing " + label, 1);

                                    file.getParentFile().mkdirs();
                                    BufferedOutputStream out = null;
                                    try {
                                        out = new BufferedOutputStream(new FileOutputStream(file));
                                        out.write(t);
                                    } catch (FileNotFoundException e) {
                                        log.error(e.getMessage(), e);
                                        userlog.logError("No directory in which to create file " + label, e);
                                    } catch (IOException e) {
                                        log.error(e.getMessage(), e);
                                        userlog.logError("Failed to create file " + label + ", " + e.getMessage(), e);
                                    } finally {
                                        if( out != null ) {
                                            try {
                                                out.close();
                                            } catch (IOException e) {
                                                log.error("Failed while closing file " + label + ": " + e.getMessage(), e);
                                                userlog.logError("Probably failed to create file " + label + ", " + e.getMessage(), e);
                                            }
                                        }
                                    } //end try/finally
                                    
                                    progressTracker.changeMessage("Wrote " + label, 1);
                                }//end if(file.exists())
                                
                                if( progressTracker.stepsRemaining() == 0 ) {
                                    progressTracker.setFinished(true);
                                }
                            }//end run()
                        });//end retrieveLabelledData(fileLabelId)
                    }//end for(fileLabelId)
                } catch (InvalidProtocolBufferException e1) {
                    log.error(e1.getMessage(), e1);
                } //end try/catch
            }//end run()
        });//end retrieveLabelledData(backupLabelId)
	}//end start()
	
	public ProgressTracker getProgressTracker() {
		return progressTracker;
	}

	public void blockUntilDone() throws InterruptedException {
		 ProgressTracker progressTracker = getProgressTracker();
		 while( !progressTracker.isFinished() && !progressTracker.isFailed() ) {
		     Thread.sleep(1000);
		 }
	}
	
	public void doRestore(String password) throws InterruptedException, IOException {
        start(getBackupConfig().getAuthenticatedOwner(password));
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
