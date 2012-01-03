package com.geekcommune.friendlybackup.integ;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
        //wait for everyone to get started listening
        Thread.sleep(100);

        testNodes[0].backup();
        
        tryRestore();
        tryRestore();

        //touch a backup file
        File f = new File("test/integ/happy2/config1/dir-to-backup/hi.txt");
        FileOutputStream fos = new FileOutputStream(f, true);
        fos.write('A');
        fos.close();
        
        testNodes[0].backup();
        
        tryRestore();
        tryRestore();
    }

    private void tryRestore() throws Exception, IOException {
        cleanDirectory(testNodes[0].getRestoreDirectory());

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
