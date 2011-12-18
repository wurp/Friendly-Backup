package com.geekcommune.friendlybackup.main;

import junit.framework.TestCase;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;

public class BackupRestoreTest extends TestCase {
    public void testBackupRestoreFakeMessageUtil() throws Exception {
        BackupMessageUtil.setInstance(new MockBackupMessageUtil());
        MessageUtil.setInstance(BackupMessageUtil.instance());
        
        String password = "password";
        System.setProperty(Action.BACKUP_CONFIG_PROP_KEY, "test/integ/happy1/config/BackupConfig.properties");

        Backup backup = new Backup();
        backup.doBackup(password);
        
        Restore restore = new Restore();
        restore.doRestore(password);
        
        //TODO
        //compareDirectories();
    }
}
