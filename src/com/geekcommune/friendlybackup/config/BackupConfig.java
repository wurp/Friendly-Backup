package com.geekcommune.friendlybackup.config;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.identity.EncryptionUtil;
import com.geekcommune.identity.KeyDataSource;
import com.geekcommune.identity.PublicIdentity;
import com.geekcommune.identity.PublicIdentityHandle;
import com.geekcommune.identity.SecretIdentity;
import com.geekcommune.util.FileUtil;
import com.geekcommune.util.Pair;

public class BackupConfig {
    private static final Logger log = Logger.getLogger(BackupConfig.class);

    static final String DELIM = "~";

    String myName;
    File backupRootDir;
    String backupStreamName;
    String computerName;
    File root;
    RemoteNodeHandle[] storingNodes;
    File restoreRootDir;
    int localPort;
    Pair<PGPPublicKeyRingCollection, PGPSecretKeyRingCollection> keyRings;
    SecretIdentity secretIdentity;
    PublicIdentity owner;
    PGPPublicKeyRing pubkeyring;
    KeyDataSource keyDataSource;
    int backupMinute;
    int backupHour;

    private char[] passphrase;

    BackupConfig() {
    }

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

	public synchronized RemoteNodeHandle[] getStoringNodes() {
		return storingNodes;
	}

	public synchronized SecretIdentity getAuthenticatedOwner() throws FriendlyBackupException {
	    //if we haven't initialized, or if the passphrase has been changed since we last tried to initialize
	    if( secretIdentity == null || passphrase != keyDataSource.getPassphrase() ) {
	        passphrase = keyDataSource.getPassphrase();
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
