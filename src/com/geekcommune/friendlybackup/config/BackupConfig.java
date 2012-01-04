package com.geekcommune.friendlybackup.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.identity.EncryptionUtil;
import com.geekcommune.identity.KeyDataSource;
import com.geekcommune.identity.PublicIdentity;
import com.geekcommune.identity.PublicIdentityHandle;
import com.geekcommune.identity.SecretIdentity;
import com.geekcommune.util.FileUtil;
import com.geekcommune.util.Pair;

public class BackupConfig {
    private static final Logger log = Logger.getLogger(BackupConfig.class);

	private static final String DELIM = "~";
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

    private String myName;

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
		retval.myName = myProps.getProperty(MY_NAME_KEY);
		
		{
	        String backupTime = myProps.getProperty(BACKUP_TIME_KEY);
	        String[] timeBits = backupTime.split(":");
            retval.backupHour = Integer.parseInt(timeBits[0]);
            retval.backupMinute = Integer.parseInt(timeBits[1]);
		}
		
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
    private Pair<PGPPublicKeyRingCollection, PGPSecretKeyRingCollection> keyRings;
    private SecretIdentity secretIdentity;
    private PublicIdentity owner;

    private PGPPublicKeyRing pubkeyring;

    private KeyDataSource keyDataSource;

    private int backupMinute;

    private int backupHour;

	public KeyDataSource getKeyDataSource() {
        return keyDataSource;
    }

    public void setKeyDataSource(KeyDataSource keyDataSource) {
        this.keyDataSource = keyDataSource;
    }

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

	public synchronized SecretIdentity getAuthenticatedOwner() throws FriendlyBackupException {
	    if( secretIdentity == null ) {
	        char[] passphrase = keyDataSource.getPassphrase();
            secretIdentity = new SecretIdentity(
                    getPublicKeyRing(),
                    getSecretKeyRingCollection(),
                    passphrase);
	    }
	    
	    return secretIdentity;
	}

	public File getRoot() {
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

    public synchronized PublicIdentity getOwner() throws FriendlyBackupException {
        if( this.owner == null ) {
            try {
                PGPPublicKeyRing pubKeyRing = getPublicKeyRing();
                PGPSecretKeyRingCollection secKeyRings =
                        getSecretKeyRingCollection();
                
                PGPSecretKey signingKey =
                        EncryptionUtil.instance().findFirstSigningKey(
                                pubKeyRing,
                                secKeyRings);

                PGPPublicKey pubEncryptingKey =
                        EncryptionUtil.instance().findFirstEncryptingKey(
                                pubKeyRing);

                this.owner = new PublicIdentity(
                        pubKeyRing,
                        new PublicIdentityHandle(
                                signingKey.getPublicKey(),
                                pubEncryptingKey));
            } catch (PGPException e) {
                throw new FriendlyBackupException("Could not find encrypting and/or signing key for owner", e);
            }
        }
        
        return owner;
    }

    private synchronized PGPPublicKeyRing getPublicKeyRing() throws FriendlyBackupException {
        if( this.pubkeyring == null ) {
            final String EXCEPTION_MESSAGE = "Failed to retrieve public keyring";

            PGPPublicKeyRingCollection pkrc;
            try {
                pkrc = getPublicKeyRingCollection();
                this.pubkeyring = EncryptionUtil.instance().findPublicKeyRing(pkrc, getMyName());
            } catch (IOException e) {
                throw new FriendlyBackupException(EXCEPTION_MESSAGE, e);
            } catch (PGPException e) {
                throw new FriendlyBackupException(EXCEPTION_MESSAGE, e);
            }
        }
        
        return this.pubkeyring;
    }

    public String getMyName() {
        return myName;
    }

    public PGPPublicKeyRingCollection getPublicKeyRingCollection()
            throws FriendlyBackupException {
        initKeyRings();
        return keyRings.getFirst();
    }

    protected PGPSecretKeyRingCollection getSecretKeyRingCollection() throws FriendlyBackupException {
        initKeyRings();
        return keyRings.getSecond();
    }

    private synchronized void initKeyRings() throws FriendlyBackupException {
        if( keyRings == null ) {
            final String EXCEPTION_MESSAGE = "Failed to initialize keyrings";

            KeyDataSource keyDataSource = new SwingUIKeyDataSource();
            try {
                keyRings = EncryptionUtil.instance().getOrCreateKeyring(
                        new File(getRoot(), "gnupg/pubring.gpg"),
                        new File(getRoot(), "gnupg/secring.gpg"),
                        keyDataSource);
            } catch (FileNotFoundException e) {
                throw new FriendlyBackupException(EXCEPTION_MESSAGE, e);
            } catch (NoSuchAlgorithmException e) {
                throw new FriendlyBackupException(EXCEPTION_MESSAGE, e);
            } catch (NoSuchProviderException e) {
                throw new FriendlyBackupException(EXCEPTION_MESSAGE, e);
            } catch (InvalidAlgorithmParameterException e) {
                throw new FriendlyBackupException(EXCEPTION_MESSAGE, e);
            } catch (PGPException e) {
                throw new FriendlyBackupException(EXCEPTION_MESSAGE, e);
            } catch (IOException e) {
                throw new FriendlyBackupException(EXCEPTION_MESSAGE, e);
            }
        }
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

    public int getBackupHour() {
        return backupHour;
    }

    public int getBackupMinute() {
        return backupMinute;
    }
}
