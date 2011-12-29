package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.log4j.Logger;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.builder.ErasureManifestBuilder;
import com.geekcommune.friendlybackup.builder.LabelledDataBuilder;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.format.high.BackupManifest;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.identity.PrivateIdentity;

/**
 * Initiate a backup according to the backup configuration specified by system property.
 * 
 * @see com.geekcommune.friendlybackup.main.Action
 * @see com.geekcommune.friendlybackup.main.Restore
 * 
 * @author Bobby Martin
 *
 */
public class Backup extends Action {
	
	private static final Logger log = Logger.getLogger(Backup.class);

	private Thread backupThread;
	private ProgressTracker progressTracker;

	public Backup() throws IOException {
	}

    /**
     * Start a backup in the background.
     * @param authenticatedOwner
     * @throws IOException
     */
    public void start(final PrivateIdentity authenticatedOwner) throws IOException {
		if( backupThread != null ) {
			throw new RuntimeException("Already started");
		}

		progressTracker = new ProgressTracker(105);

		final BackupConfig bakcfg = App.getBackupConfig();

		backupThread = new Thread(new Runnable() {
			public void run() {
			    try {
	                doBackupInternal(bakcfg, authenticatedOwner, makeExpiryDate());
			    } catch(Exception e) {
			        log.error(e.getMessage(), e);
			    }
			}
		});

		backupThread.start();
	}

    /**
     * Completes the backup (or errors out) before returning.
     * @param password
     * @throws IOException
     * @throws InterruptedException
     */
	public void doBackup(String password) throws IOException, InterruptedException {
	    start(App.getBackupConfig().getAuthenticatedOwner(password));
	    backupThread.join();

        while( !progressTracker.isFinished() && !progressTracker.isFailed() ) {
            System.out.println(progressTracker.getStatusMessage());
            Thread.sleep(1000);
        }
	}

	/**
	 * Queues up all the backup messages to be sent.
	 * @param bakcfg
	 * @param authenticatedOwner
	 * @param expiryDate
	 */
	protected void doBackupInternal(BackupConfig bakcfg, PrivateIdentity authenticatedOwner, Date expiryDate) {
		UserLog userlog = UserLog.instance();

		//first make sure there are no other messages still hanging around from previous backups
		progressTracker.changeMessage("cleaning out any messages from last backup", 1);
		BackupMessageUtil.instance().cleanOutBackupMessageQueue();
		RemoteNodeHandle[] storingNodes = bakcfg.getStoringNodes();

		BackupManifest bakman = new BackupManifest(new Date());

		progressTracker.changeMessage("Building list of files to back up", 1);
		List<File> files = bakcfg.getFilesToBackup();

		progressTracker.progress(3);

		//Update progress meter to track based on # of files to be backed up
		//assume upload will take 3x as long as building the messages, and allot 4 for sending the backup manifest
		progressTracker.rebase(files.size() * 5 + 4);

		for(File f : files) {
			try {
				progressTracker.changeMessage("Building messages for " + f.getCanonicalPath(), 1);
				ErasureManifest erasureManifest = ErasureManifestBuilder.instance().buildFromFile(
						storingNodes,
						f,
						bakcfg,
						expiryDate,
						authenticatedOwner,
						progressTracker.createSubTracker(3));

				LabelledData labelledData = LabelledDataBuilder.buildLabelledData(
						authenticatedOwner,
						bakcfg.getFullFilePath(f),
						erasureManifest.getHashID(),
						storingNodes,
						bakcfg.getLocalPort(),
						expiryDate,
                        progressTracker.createSubTracker(1));

				bakman.add(labelledData.getHashID());
			} catch(Exception e) {
				String filePath = "unknown file";
				try {
					filePath = f.getCanonicalPath();
				} catch(Exception e2) {
					log.error(e2);
				}
				String msg = "Failed to back up " + filePath;
				userlog.logError(msg, e);
				log.error(msg + ": " + e.getMessage(), e);
			} //end try/catch
		} //end for

        ErasureManifest erasureManifest = ErasureManifestBuilder.instance().buildFromBytes(
                storingNodes,
                bakman.getData(),
                bakcfg,
                expiryDate,
                authenticatedOwner,
                progressTracker.createSubTracker(3));

        LabelledDataBuilder.buildLabelledData(
                authenticatedOwner,
                bakcfg.getBackupStreamName(),
                erasureManifest.getHashID(),
                storingNodes,
                bakcfg.getLocalPort(),
                expiryDate,
                progressTracker.createSubTracker(1));
	}
	
	private Date makeExpiryDate() {
		Date expiryDate = new Date(System.currentTimeMillis() + 180 * MILLIS_PER_DAY);
		return expiryDate;
	}
}
