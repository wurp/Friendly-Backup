package com.geekcommune.friendlybackup.config;

import junit.framework.Assert;
import junit.framework.TestCase;

import com.geekcommune.friendlybackup.main.App;
import com.geekcommune.identity.SecretIdentity;

public class BackupConfigTest extends TestCase {
    public void setUp() throws Exception {
        App.wire("test/integ/happy2/config1/BackupConfig.properties");
    }
    
    public void testKeyInitialization() throws Exception {
        ((SwingUIKeyDataSource)App.getBackupConfig().getKeyDataSource()).
            setPassphrase("password".toCharArray());
        SecretIdentity authOwner =
                App.getBackupConfig().getAuthenticatedOwner();
        Assert.assertNotNull(authOwner);
    }
}
