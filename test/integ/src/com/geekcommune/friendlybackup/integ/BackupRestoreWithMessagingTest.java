package com.geekcommune.friendlybackup.integ;

import junit.framework.Assert;

public class BackupRestoreWithMessagingTest extends IntegrationTestCase {
    private TestNode[] testNodes;
    
    public void setUp() throws Exception {
        testNodes = makeTestNodes(new String[] {
                "test/integ/happy2/config1/BackupConfig.properties",
                "test/integ/happy2/config2/BackupConfig.properties",
                "test/integ/happy2/config3/BackupConfig.properties",
                "test/integ/happy2/config4/BackupConfig.properties",
                });
    }

    public void testBackupRestoreFakeMessageUtil() throws Exception {
        
        cleanDirectory(testNodes[0].getRestoreDirectory());
        
        testNodes[0].backup();
        
        testNodes[0].restore();
        
        Assert.assertTrue(
                "restored directory not identical to original",
                compareDirectories(
                        testNodes[0].getBackupRootDirectories()[0],
                        testNodes[0].getRestoreDirectory()));
    }

    private TestNode[] makeTestNodes(String[] configFilePaths) throws Exception {
        TestNode[] retval = new TestNode[configFilePaths.length];

        for(int i = 0; i < configFilePaths.length; ++i) {
            retval[i] = makeTestNode(configFilePaths[i]);
        }

        return retval;
    }

    private TestNode makeTestNode(String configFilePath) throws Exception {
        return new TestNode(configFilePath);
    }
    
}
