package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.builder.ErasureManifestBuilder;
import com.geekcommune.friendlybackup.builder.LabelledDataBuilder;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.erasurefinder.UserLog;
import com.geekcommune.friendlybackup.format.high.BackupManifest;
import com.geekcommune.friendlybackup.format.low.ErasureManifest;
import com.geekcommune.friendlybackup.format.low.LabelledData;
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
	private Thread thread;
	private ProgressTracker progressTracker;

	public void start(final PrivateIdentity authenticatedOwner) throws IOException {
		if( thread != null ) {
			throw new RuntimeException("Already started");
		}
		
		progressTracker = new ProgressTracker(105);

		final BackupConfig bakcfg = getBackupConfig();
		
		thread = new Thread(new Runnable() {
			public void run() {
			    try {
	                doBackupInternal(bakcfg, authenticatedOwner, makeExpiryDate());
			    } catch(Exception e) {
			        log.error(e.getMessage(), e);
			    }
			}
		});
		
		thread.start();
	}
	
	public void doBackup(String password) throws IOException, InterruptedException {
	    start(getBackupConfig().getAuthenticatedOwner(password));
	    thread.join();
	}
	
	protected void doBackupInternal(BackupConfig bakcfg, PrivateIdentity authenticatedOwner, Date expiryDate) {
		UserLog userlog = UserLog.instance();
		
		//first make sure there's no other messages still hanging around from previous backups
		progressTracker.changeMessage("cleaning out any messages from last backup", 1);
		BackupMessageUtil.instance().cleanOutBackupMessageQueue();
		RemoteNodeHandle[] storingNodes = bakcfg.getStoringNodes();

		BackupManifest bakman = new BackupManifest(bakcfg.getBackupStreamName(), new Date());

		progressTracker.changeMessage("Building list of files to back up", 1);
		List<File> files = bakcfg.getFilesToBackup();
		
		progressTracker.progress(3);
		
		//Update progress meter to track based on # of files to be backed up
		//assume upload will take 3x as long as building the messages, add 1 more for interacting with each storing node to figure out what to upload
		progressTracker.rebase(files.size() * 4 + storingNodes.length);
		
		for(File f : files) {
			try {
				progressTracker.changeMessage("Building messages for " + f.getCanonicalPath(), 1);
				ErasureManifest erasureManifest = ErasureManifestBuilder.instance().buildFromFile(
						storingNodes,
						f,
						bakcfg.getErasuresNeeded(),
						bakcfg.getTotalErasures());

				LabelledData labelledData = LabelledDataBuilder.buildLabelledData(
						authenticatedOwner,
						bakcfg.getFullFilePath(f),
						erasureManifest.getHashID(),
						storingNodes,
						expiryDate);

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
			}

			ErasureManifest erasureManifest = ErasureManifestBuilder.instance().buildFromBytes(
					storingNodes,
					bakman.getData(),
					bakcfg.getErasuresNeeded(),
					bakcfg.getTotalErasures());
			
			LabelledDataBuilder.buildLabelledData(
					authenticatedOwner,
					bakcfg.getBackupStreamName(),
					erasureManifest.getHashID(),
					storingNodes,
					expiryDate);
		}
		
		//now just process the message uploads
		BackupMessageUtil.instance().processBackupMessages(progressTracker);
	}
	
	private Date makeExpiryDate() {
		Date expiryDate = new Date(System.currentTimeMillis() + 180 * MILLIS_PER_DAY);
		return expiryDate;
	}
}
