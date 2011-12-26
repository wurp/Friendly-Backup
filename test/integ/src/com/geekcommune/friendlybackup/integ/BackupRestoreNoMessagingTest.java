package com.geekcommune.friendlybackup.integ;

import junit.framework.Assert;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.main.App;
import com.geekcommune.friendlybackup.main.Backup;
import com.geekcommune.friendlybackup.main.MockBackupMessageUtil;
import com.geekcommune.friendlybackup.main.Restore;

public class BackupRestoreNoMessagingTest extends IntegrationTestCase {
    public void testBackupRestoreFakeMessageUtil() throws Exception {
        String password = "password";

        Backup backup = new Backup();
        backup.doBackup(password);
        
        Restore restore = new Restore();
        restore.doRestore(password);

        Assert.assertTrue(compareDirectories(
                App.getBackupConfig().getBackupRootDirectories()[0],
                App.getBackupConfig().getRestoreRootDirectory()));
    }

    public void setUp() throws Exception {
        System.setProperty(App.BACKUP_CONFIG_PROP_KEY, "test/integ/happy1/config/BackupConfig.properties");
        App.wire();
        
        BackupMessageUtil.setInstance(new MockBackupMessageUtil(App.getBackupConfig()));
        MessageUtil.setInstance(BackupMessageUtil.instance());
        
        cleanDirectory(App.getBackupConfig().getRestoreRootDirectory());
    }
}
