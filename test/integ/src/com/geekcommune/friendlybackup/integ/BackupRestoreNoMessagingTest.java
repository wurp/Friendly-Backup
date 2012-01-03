package com.geekcommune.friendlybackup.integ;

import junit.framework.Assert;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.config.SwingUIKeyDataSource;
import com.geekcommune.friendlybackup.main.App;
import com.geekcommune.friendlybackup.main.Backup;
import com.geekcommune.friendlybackup.main.MockBackupMessageUtil;
import com.geekcommune.friendlybackup.main.Restore;

public class BackupRestoreNoMessagingTest extends IntegrationTestCase {
    public void testBackupRestoreFakeMessageUtil() throws Exception {

        Backup backup = new Backup();
        backup.doBackup();
        
        Restore restore = new Restore();
        restore.doRestore();

        Assert.assertTrue(compareDirectories(
                App.getBackupConfig().getBackupRootDirectories()[0],
                App.getBackupConfig().getRestoreRootDirectory()));
    }

    public void setUp() throws Exception {
        System.setProperty(App.BACKUP_CONFIG_PROP_KEY, "test/integ/happy1/config/BackupConfig.properties");
        App.wire();

        char[] passphrase = "password".toCharArray();
        ((SwingUIKeyDataSource)App.getBackupConfig().getKeyDataSource()).
            setPassphrase(passphrase);

        BackupMessageUtil.setInstance(new MockBackupMessageUtil(App.getBackupConfig()));
        MessageUtil.setInstance(BackupMessageUtil.instance());
        
        cleanDirectory(App.getBackupConfig().getRestoreRootDirectory());
    }
}
