package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.bouncycastle.openpgp.PGPException;

import com.geekcommune.communication.message.ClientStartupMessage;
import com.geekcommune.communication.message.ConfirmationMessage;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.config.SwingCreateAccountDialog;
import com.geekcommune.friendlybackup.config.SwingUIKeyDataSource;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.friendlybackup.server.format.high.ClientUpdate;
import com.geekcommune.identity.EncryptionUtil;

public class FBNodeApp extends App {
    private Backup backup;
    private Restore restore;
    private Date nextBackupTime;
    private File restoreFile;
    private File backupFile;

    /**
     * Ask the user for the passphrase
     */
    public FBNodeApp() {
    	this(null, null);
    }

    /**
     * Ask the user for the passphrase
     */
    public FBNodeApp(String configFilePath) {
    	this(null, configFilePath);
    }

    public FBNodeApp(char[] passphrase, String configFilePath) {
        try {
        	if( configFilePath == null ) {
                wire();
        	} else {
                wire(configFilePath);
        	}

            //if no secret keyring, create one
            createKeyringIfNeeded();
            
            authenticateUser(passphrase);
            
            updateServer();
            
            restoreFile = new File(getBackupConfig().getRoot(), "restore.txt");
            backupFile = new File(getBackupConfig().getRoot(), "backup.txt");
            
            backup = new Backup(getBackupConfig());
            restore = new Restore(getBackupConfig());

            nextBackupTime = findNextBackupTime();
            UserLog.instance().logInfo("Next backup at " + nextBackupTime);
        } catch(IOException e) {
            UserLog.instance().logError("Could not start service", e);
            System.exit(-1);
        } catch (FriendlyBackupException e) {
            UserLog.instance().logError(e.getMessage(), e);
            System.exit(-1);
        } catch (InterruptedException e) {
            UserLog.instance().logError("Could not generate keys", e);
        }
    }

	private void updateServer() throws FriendlyBackupException, IOException {
		ClientUpdate cu = new ClientUpdate(
				getBackupConfig().getMyName(),
				getBackupConfig().getEmail(),
				getBackupConfig().getRoot().getFreeSpace(),
				getBackupConfig().getPublicKeyRing().getEncoded(),
				getBackupConfig().getAuthenticatedOwner());
			;

		ClientStartupMessage csm = new ClientStartupMessage(
				getBackupConfig().getServerAddress(),
				getBackupConfig().getLocalPort(),
				cu);

		BackupMessageUtil.instance().queueMessage(csm);
		
		//block for the response
		try {
			if( !csm.awaitResponse(30000) ) {
			    throw new FriendlyBackupException("Timed out waiting for " + getBackupConfig().getServerAddress());
			}
		} catch (InterruptedException e) {
			throw new FriendlyBackupException("Interrupted while waiting for response from server", e);
		}
		
		ConfirmationMessage confirmationMsg = csm.getConfirmation();
		if( !confirmationMsg.isOK() ) {
			throw new FriendlyBackupException("Could not register with server: " +
					confirmationMsg.getErrorMessage());
		}
	}

	private void createKeyringIfNeeded() throws IOException,
			InterruptedException {
		if( !getBackupConfig().getSecretKeyringFile().isFile() ) {
		    SwingCreateAccountDialog createAccountDialog = new SwingCreateAccountDialog();

		    if( createAccountDialog.getPassphrase() == null ) {
		        UserLog.instance().logInfo("Exiting at user's request");
		        System.exit(-1);
		    }

		    String name = createAccountDialog.getName();
		    String email = createAccountDialog.getEmail();
		    char[] passphrase = createAccountDialog.getPassphrase();
		    
		    getBackupConfig().setMyName(name);
		    getBackupConfig().setEmail(email);
		    ((SwingUIKeyDataSource)getBackupConfig().getKeyDataSource()).setPassphrase(passphrase);
		    
		    EncryptionUtil.instance().generateKey(
		            name,
		            email,
		            passphrase,
		            getBackupConfig().getPublicKeyringFile(),
		            getBackupConfig().getSecretKeyringFile());
		}
	}

	private void authenticateUser(char[] passphrase) throws FriendlyBackupException {
		boolean passphraseCorrect = false;
		while(!passphraseCorrect) {
		    //make sure we have the passphrase now, since the user is presumably at the computer
		    //Empty passphrase is interpreted as meaning "quit"
		    if( getBackupConfig().getKeyDataSource().getPassphrase() == null ) {
		        UserLog.instance().logInfo("Exiting at user's request");
		        System.exit(-1);
		    }
		    
		    //verify that the passphrase is correct
		    try {
		        getBackupConfig().getAuthenticatedOwner().sign(new byte[] { 1, 2, 3, 4, 5 });
		        passphraseCorrect = true;
		    } catch (FriendlyBackupException e) {
		        UserLog.instance().logError("pwd error", e);
		        if( e.getCause() instanceof PGPException ) {
		            getBackupConfig().getKeyDataSource().clearPassphrase();
		        } else {
		            throw e;
		        }
		    }
		}
	}
    
    public static void main(String[] args) throws Exception {
        FBNodeApp svc;
    	if( args.length == 0) {
    		svc = new FBNodeApp();
    	} else {
    		svc = new FBNodeApp(args[0]);
    	}
        svc.go();
    }

    public void go() {
        //check once every 5 seconds to see if I should do something
        for(;;) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                UserLog.instance().logError("", e);
            }
            
            //do a restore if restore.txt exists in same directory as BackupConfig.properties
            if( restoreFile.isFile() ) {
                restoreFile.delete();
                
                try {
                    restore();
                } catch (FriendlyBackupException e) {
                    UserLog.instance().logError("Restore failed", e);
                } catch (InterruptedException e) {
                    UserLog.instance().logError("Restore failed", e);
                }
            }
            
            //do a backup if backup.txt exists in same directory as BackupConfig.properties
            boolean doBackup = false;
            if( backupFile.isFile() ) {
                backupFile.delete();
                doBackup = true;
            }
            
            Date timestamp = new Date();
            if( doBackup || timestamp.after(nextBackupTime) ) {
                try {
                    backup();
                } catch (IOException e) {
                    UserLog.instance().logError("Backup failed", e);
                } catch (InterruptedException e) {
                    UserLog.instance().logError("Backup failed", e);
                }
                nextBackupTime = findNextBackupTime();
                UserLog.instance().logInfo("Next backup at " + nextBackupTime);
            }
        }
    }

    public void backup() throws IOException, InterruptedException {
        backup.doBackup();
    }

    public void restore() throws FriendlyBackupException, InterruptedException {
        restore.doRestore();
    }

    /**
     * Find the earliest future backup time.
     * @return
     */
    public Date findNextBackupTime() {
        int backupHour = getBackupConfig().getBackupHour();
        int backupMinute = getBackupConfig().getBackupMinute();
        
        GregorianCalendar retval = new GregorianCalendar();
        
        //if it's already past backup time, move to tomorrow
        int currHour = retval.get(GregorianCalendar.HOUR_OF_DAY);
        int currMin = retval.get(GregorianCalendar.MINUTE);
        if( currHour > backupHour ||
                (currHour == backupHour && currMin >= backupMinute)  ) {
            retval.add(GregorianCalendar.DATE, 1);
        }
        
        //set the backup time
        retval.set(GregorianCalendar.HOUR_OF_DAY, backupHour);
        retval.set(GregorianCalendar.MINUTE, backupMinute);
        retval.set(GregorianCalendar.SECOND, 0);
        
        return retval.getTime();
    }
}
