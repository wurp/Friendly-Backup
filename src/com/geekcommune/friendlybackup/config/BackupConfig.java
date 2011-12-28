package com.geekcommune.friendlybackup.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.identity.PrivateIdentity;
import com.geekcommune.identity.PublicIdentity;
import com.geekcommune.util.FileUtil;

public class BackupConfig {
    private static final Logger log = Logger.getLogger(BackupConfig.class);

	private static final String DELIM = "~";
    private static final String FRIENDS_KEY = "friends";
    private static final String BACKUP_ROOT_DIR_KEY = "backupRootDir";
    private static final String RESTORE_ROOT_DIR_KEY = "restoreRootDir";
    private static final String LOCAL_PORT_KEY = "port";
    private static final String BACKUP_STREAM_NAME_KEY = "backupName";
    private static final String COMPUTER_NAME_KEY = "computerName";
    private static final String FRIEND_PREFIX = "friend.";
    private static final String EMAIL_SUFFIX = ".email";
    private static final String CONNECT_INFO_SUFFIX = ".connectinfo";

    public BackupConfig() {
    }

    public static BackupConfig parseConfigFile(File backupConfig) throws IOException {
		Properties myProps = new Properties();
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(backupConfig));
		try {
		    myProps.load(bis);
		} finally {
		    bis.close();
		}

		BackupConfig retval = new BackupConfig();
		//populate from properties
		retval.root = backupConfig.getParentFile();
		//TODO not sure if this will work when backupRootDir is an absolute path
        retval.backupRootDir = new File(retval.root, myProps.getProperty(BACKUP_ROOT_DIR_KEY));
        retval.restoreRootDir = new File(retval.root, myProps.getProperty(RESTORE_ROOT_DIR_KEY));
        retval.localPort = Integer.parseInt(myProps.getProperty(LOCAL_PORT_KEY));
		retval.backupStreamName = myProps.getProperty(BACKUP_STREAM_NAME_KEY);
		retval.computerName = myProps.getProperty(COMPUTER_NAME_KEY);
		
		String[] friends = myProps.getProperty(FRIENDS_KEY).split(",");
		retval.storingNodes = new RemoteNodeHandle[friends.length];
		for(int i = 0; i < friends.length; ++i) {
		    retval.storingNodes[i] = new RemoteNodeHandle(
		            friends[i],
		            myProps.getProperty(FRIEND_PREFIX+friends[i]+EMAIL_SUFFIX),
		            myProps.getProperty(FRIEND_PREFIX+friends[i]+CONNECT_INFO_SUFFIX));
		}

		retval.validate();
		return retval;
	}

    private void validate() {
        if( computerName.indexOf(DELIM) != -1 ) {
            UserLog.instance().logError("Computer name may not contain " + DELIM);
            throw new RuntimeException("Computer name may not contain " + DELIM);
        }
        
    }

    private File backupRootDir;
    private String backupStreamName;
    private String computerName;
    private File root;
    private RemoteNodeHandle[] storingNodes;
    private File restoreRootDir;

    private int localPort;

	//a name for the series of backups this configuration configures
	public String getBackupStreamName() {
		return backupStreamName;
	}

    public String getFullFilePath(File f) throws IOException {
        return getComputerName() + DELIM + f.getCanonicalPath();
    }

    /**
     * For valid fullFilePath, will return computer name as the 0th element
     * and full file path as the 1st element.
     * @param fullFilePath
     * @return
     */
    public String[] parseFullFilePath(String fullFilePath) {
        return fullFilePath.split(DELIM);
    }

	public String getComputerName() {
		return computerName;
	}

	public int getTotalErasures() {
		return 60;
	}

	public int getErasuresNeeded() {
		return 40;
	}

	public RemoteNodeHandle[] getStoringNodes() {
		return storingNodes;
	}

	public PrivateIdentity getAuthenticatedOwner(String password) {
		// TODO Auto-generated method stub
		return new PrivateIdentity();
	}

	private File getRoot() {
	    return root;
	}
	
	public List<File> getFilesToBackup() {
		File root = getBackupRootDir();
        List<File> retval = new ArrayList<File>();

        FileUtil.instance().listTree(retval, root, new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
		
        return retval;
	}

    /**
     * Get the directories to be backed up.
     * Note that some subtrees may have blacklisted subtrees or files within them.
     * @return
     */
	public File[] getBackupRootDirectories() {
	    return new File[] { getBackupRootDir() };
	}
	
	/**
	 * Get the directory to be backed up.  This is temporary
	 * until I get the infrastructure in place to pull from many directories, with the ability to blacklist subtrees.
	 * @return
	 */
    private File getBackupRootDir() {
        return backupRootDir;
    }

    public String getDbConnectString() {
        return "jdbc:hsqldb:file:" + getDbFile().getAbsolutePath().replace('\\', '/');
    }

    private File getDbFile() {
        File dbDir = new File(getRoot(), "db");
        dbDir.mkdirs();
        return new File(dbDir, "friendbackups");
    }

    public PublicIdentity getOwner() {
        //TODO
        return new PublicIdentity();
    }

    public File getRestoreRootDirectory() {
        return restoreRootDir;
    }

    public String getRestorePath(String label) throws IOException {
        String path = parseFullFilePath(label)[1];
        String backupRoot = getBackupRootDirectories()[0].getCanonicalPath();

        if( path.startsWith(backupRoot)) {
            path = path.substring(backupRoot.length());
            while( path.startsWith("/") || path.startsWith("\\") ) {
                path = path.substring(1);
            }
            
            path = new File(getRestoreRootDirectory(), path).getCanonicalPath();
        } else {
            log.warn("Expected path " + path + " to start with " + backupRoot);
        }

        return path;
    }

    public int getLocalPort() {
        return localPort;
    }

    public RemoteNodeHandle getFriend(InetAddress inetAddress,
            int originNodePort) {
        for(RemoteNodeHandle storingNode : storingNodes) {
            if( inetAddress.equals(storingNode.getAddress()) && originNodePort == storingNode.getPort() ) {
                return storingNode;
            }
        }

        return null;
    }
}
