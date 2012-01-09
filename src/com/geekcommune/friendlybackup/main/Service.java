package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import org.bouncycastle.openpgp.PGPException;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.config.SwingCreateAccountDialog;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.identity.EncryptionUtil;

public class Service extends App {
    private Backup backup;
    private Restore restore;
    private Date nextBackupTime;
    private File restoreFile;

    public Service() {
        try {
            wire();

            //if no secret keyring, create one
            if( !getBackupConfig().getSecretKeyringFile().isFile() ) {
                SwingCreateAccountDialog createAccountDialog = new SwingCreateAccountDialog();

                if( createAccountDialog.getPassphrase() == null ) {
                    UserLog.instance().logInfo("Exiting at user's request");
                    System.exit(-1);
                }

                String name = createAccountDialog.getName();
                String email = createAccountDialog.getEmail();
                char[] passphrase = createAccountDialog.getPassphrase();
                
                EncryptionUtil.instance().generateKey(
                        name,
                        email,
                        passphrase,
                        getBackupConfig().getPublicKeyringFile(),
                        getBackupConfig().getSecretKeyringFile());
            }
            
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
            
            restoreFile = new File(getBackupConfig().getRoot(), "restore.txt");
            
            backup = new Backup();
            restore = new Restore();

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
        Service svc = new Service();
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
                    restore.doRestore();
                } catch (FriendlyBackupException e) {
                    UserLog.instance().logError("Restore failed", e);
                } catch (InterruptedException e) {
                    UserLog.instance().logError("Restore failed", e);
                }
            }
            
            Date timestamp = new Date();
            if( timestamp.after(nextBackupTime) ) {
                try {
                    backup.doBackup();
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
