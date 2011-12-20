package com.geekcommune.friendlybackup.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.identity.PrivateIdentity;
import com.geekcommune.identity.PublicIdentity;

public class BackupConfig {

	private static final String DELIM = "~";
    private static final String FRIENDS_KEY = "friends";
    private static final String BACKUP_ROOT_DIR_KEY = "backupRootDir";
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
	
	/**
	 * Populates result with all files in the tree rooted at 'root' which match ff.  Depth first recursion.
	 * 
	 * @param result
	 * @param root
	 * @param ff
	 */
	public static void listTree(final List<File> result, File root, final FileFilter ff) {
        File[] files = root.listFiles(new FileFilter() {
            
            public boolean accept(File pathname) {
                if( pathname.isDirectory() ) {
                    listTree(result, pathname, ff);
                }
                
                return ff.accept(pathname);
            }
        });
        
        if( files != null ) {
            for(File f : files) {
                result.add(f);
            }
        }
	}
	
	public List<File> getFilesToBackup() {
		File root = getBackupRootDir();
        List<File> retval = new ArrayList<File>();

        listTree(retval, root, new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isFile();
            }
        });
		
        return retval;
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
}
