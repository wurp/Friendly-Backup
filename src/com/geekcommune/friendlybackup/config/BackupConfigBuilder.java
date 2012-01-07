package com.geekcommune.friendlybackup.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.logging.UserLog;

public class BackupConfigBuilder {
    private static final String FRIENDS_KEY = "friends";
    private static final String BACKUP_ROOT_DIR_KEY = "backupRootDir";
    private static final String RESTORE_ROOT_DIR_KEY = "restoreRootDir";
    private static final String LOCAL_PORT_KEY = "port";
    private static final String BACKUP_STREAM_NAME_KEY = "backupName";
    private static final String COMPUTER_NAME_KEY = "computerName";
    private static final String MY_NAME_KEY = "myName";
    private static final String BACKUP_TIME_KEY = "dailyBackupTime";

    private static final String FRIEND_PREFIX = "friend.";
    private static final String EMAIL_SUFFIX = ".email";
    private static final String CONNECT_INFO_SUFFIX = ".connectinfo";

    private static String[] friends;

    private Properties myProps;
    private BackupConfig retval;

    public void parseConfigFile(File backupConfig) throws IOException, FriendlyBackupException {
        myProps = new Properties();
        
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(backupConfig));
        try {
            myProps.load(bis);
        } finally {
            bis.close();
        }

        retval = new BackupConfig();
        
        //populate from properties
        initFromProps(backupConfig);

        validate();
    }

    void validate() {
        if( retval.computerName.indexOf(BackupConfig.DELIM) != -1 ) {
            UserLog.instance().logError("Computer name may not contain " + BackupConfig.DELIM);
            throw new RuntimeException("Computer name may not contain " + BackupConfig.DELIM);
        }
    }

    private void initFromProps(File backupConfig) throws FriendlyBackupException, UnknownHostException {
        retval.root = backupConfig.getParentFile();
        //TODO not sure if this will work when backupRootDir is an absolute path
        retval.backupRootDir = new File(retval.root, myProps.getProperty(BACKUP_ROOT_DIR_KEY));
        retval.restoreRootDir = new File(retval.root, myProps.getProperty(RESTORE_ROOT_DIR_KEY));
        retval.localPort = Integer.parseInt(myProps.getProperty(LOCAL_PORT_KEY));
        retval.backupStreamName = myProps.getProperty(BACKUP_STREAM_NAME_KEY);
        retval.computerName = myProps.getProperty(COMPUTER_NAME_KEY);
        retval.myName = myProps.getProperty(MY_NAME_KEY);
        
        //Make sure system is ready to run
        if( "MyNickName".equals(retval.getMyName()) ) {
            throw new FriendlyBackupException("Edit " + retval.getRoot().getAbsolutePath() + "/BackupConfig.properties and set the values appropriately (myName cannot be MyNickName).\nSee http://bobbymartin.name/friendlybackup/properties.html");
        }
        
        //Tell the user if they don't have their keys set up
        File secringFile = new File(retval.getRoot(), "gnupg/secring.gpg");
        if( !secringFile.isFile() ) {
            UserLog.instance().logError("Install gpg or GNU Privacy Assistant, create a key suitable for encryption & signing, and copy pubring.gpg and secring.gpg to " +
                    secringFile.getParentFile().getAbsolutePath() + ".\nSee http://bobbymartin.name/friendlybackup/keygen.html");
            System.exit(-1);
        }
        
        {
            String backupTime = myProps.getProperty(BACKUP_TIME_KEY);
            String[] timeBits = backupTime.split(":");
            retval.backupHour = Integer.parseInt(timeBits[0]);
            retval.backupMinute = Integer.parseInt(timeBits[1]);
        }
        
        friends = myProps.getProperty(FRIENDS_KEY).split(",");
        initStoringNodes();
    }


    private void initStoringNodes() throws FriendlyBackupException {
        if( retval.storingNodes == null ) {
            retval.storingNodes = new RemoteNodeHandle[friends.length];
            for(int i = 0; i < friends.length; ++i) {
                String email = myProps.getProperty(FRIEND_PREFIX+friends[i]+EMAIL_SUFFIX);
                String connectInfo = myProps.getProperty(FRIEND_PREFIX+friends[i]+CONNECT_INFO_SUFFIX);

                try {
                    retval.storingNodes[i] = new RemoteNodeHandle(
                            friends[i],
                            email,
                            connectInfo);
                } catch (UnknownHostException e) {
                    throw new FriendlyBackupException("Please double check the host name in " + connectInfo, e);
                }
            }
        }
    }
    
    /**
     * You must have called parseConfigFile first...
     * @return
     * @throws FriendlyBackupException 
     */
    public BackupConfig makeBackupConfig() throws FriendlyBackupException {
        if( myProps == null ) {
            throw new FriendlyBackupException("Attempt to make BackupConfig without first parsing properties file");
        }
        
        return retval;
    }
}
