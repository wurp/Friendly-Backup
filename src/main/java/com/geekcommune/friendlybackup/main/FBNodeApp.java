package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import com.geekcommune.communication.message.AbstractMessage;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.communication.message.RetrieveDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendErasureMessage;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.datastore.DBDataStore;
import com.geekcommune.friendlybackup.datastore.DataStore;
import com.geekcommune.friendlybackup.logging.LoggingUserLog;
import com.geekcommune.friendlybackup.logging.UserLog;

public class FBNodeApp {
    public static final String BACKUP_CONFIG_PROP_KEY = "BackupConfigFile";

    private boolean wired = false;
    private Date nextBackupTime;
    private File restoreFile;
    private File backupFile;
    private FBNodeImpl fbNode;

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
            fbNode.createKeyringIfNeeded();
            
            fbNode.authenticateUser(passphrase);
            
            fbNode.updateServer();
            
            restoreFile = new File(fbNode.getBackupConfig().getRoot(), "restore.txt");
            backupFile = new File(fbNode.getBackupConfig().getRoot(), "backup.txt");
            
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
                    fbNode.restore();
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
                    fbNode.backup();
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

    /**
     * Find the earliest future backup time.
     * @return
     */
    public Date findNextBackupTime() {
        int backupHour = fbNode.getBackupConfig().getBackupHour();
        int backupMinute = fbNode.getBackupConfig().getBackupMinute();
        
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

    /**
     * Dependency Injection
     * @throws IOException 
     * @throws FriendlyBackupException 
     */
    public synchronized void wire() throws IOException, FriendlyBackupException {
        wire(System.getProperty(BACKUP_CONFIG_PROP_KEY));
    }

    /**
     * Dependency Injection
     * @throws IOException 
     * @throws FriendlyBackupException 
     */
    public synchronized void wire(String configFilePath) throws IOException, FriendlyBackupException {
        if( !wired ) {
            wired = true;
            
            //UserLog
            UserLog.setInstance(new LoggingUserLog());

            //BackupConfig
            File cfgFile = new File(configFilePath);
            fbNode = new FBNodeImpl(new BackupConfig(cfgFile));

            //DataStore
            DataStore.setInstance(new DBDataStore(fbNode.getBackupConfig().getDbConnectString()));
            
            //BackupMessageUtil
            BackupMessageUtil.setInstance(new BackupMessageUtil());
            BackupMessageUtil.instance().setBackupConfig(fbNode.getBackupConfig());
            BackupMessageUtil.instance().startListenThread();
            
            //set up messages to be read from the input data
            AbstractMessage.registerMessageFactory(VerifyMaybeSendDataMessage.INT_TYPE, VerifyMaybeSendDataMessage.FACTORY);
            AbstractMessage.registerMessageFactory(RetrieveDataMessage.INT_TYPE, RetrieveDataMessage.FACTORY);
            AbstractMessage.registerMessageFactory(VerifyMaybeSendErasureMessage.INT_TYPE, VerifyMaybeSendErasureMessage.FACTORY);
        }
    }

    /**
     * convenience method to get the backup config from the FBNode api.
     * @return
     */
    public BackupConfig getBackupConfig() {
        return fbNode.getBackupConfig();
    }
}
