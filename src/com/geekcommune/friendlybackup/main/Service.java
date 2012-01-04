package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.logging.UserLog;

public class Service extends App {
    private Backup backup;
    private Restore restore;
    private Date nextBackupTime;
    private File restoreFile;

    public Service() {
        try {
            wire();

            //Make sure system is ready to run
            if( "MyNickName".equals(getBackupConfig().getMyName()) ) {
                UserLog.instance().logError("Edit " + getBackupConfig().getRoot().getAbsolutePath() + "/BackupConfig.properties and set the values appropriately (myName cannot be MyNickName).\nSee http://bobbymartin.name/friendlybackup/properties.html");
                System.exit(-1);
            }
            
            File secringFile = new File(getBackupConfig().getRoot(), "gnupg/secring.gpg");
            if( !secringFile.isFile() ) {
                UserLog.instance().logError("Install gpg or GNU Privacy Assistant, create a key suitable for encryption & signing, and copy pubring.gpg and secring.gpg to " +
                        secringFile.getParentFile().getAbsolutePath() + ".\nSee http://bobbymartin.name/friendlybackup/keygen.html");
                System.exit(-1);
            }
            
            //make sure we have the passphrase now, since the user is presumably at the computer
            getBackupConfig().getKeyDataSource().getPassphrase();
            
            restoreFile = new File(getBackupConfig().getRoot(), "restore.txt");
            
            backup = new Backup();
            restore = new Restore();

            nextBackupTime = findNextBackupTime();
            UserLog.instance().logInfo("Next backup at " + nextBackupTime);
        } catch(IOException e) {
            
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
