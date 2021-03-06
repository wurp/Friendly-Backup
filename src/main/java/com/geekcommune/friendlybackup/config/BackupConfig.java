package com.geekcommune.friendlybackup.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
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
import com.geekcommune.util.ObjectUtil;
import com.geekcommune.util.Pair;

public class BackupConfig {
    private static final String SERVER_EMAIL = "backupserver1@geekcommune.com";

    private static final String SERVER_NAME = "backupserver";

    private static final Logger log = LogManager.getLogger(BackupConfig.class);

    private static final String FRIENDS_KEY = "friends";
    private static final String BACKUP_ROOT_DIR_KEY = "backupRootDir";
    private static final String RESTORE_ROOT_DIR_KEY = "restoreRootDir";
    private static final String LOCAL_PORT_KEY = "port";
    private static final String BACKUP_STREAM_NAME_KEY = "backupName";
    private static final String COMPUTER_NAME_KEY = "computerName";
    private static final String MY_NAME_KEY = "myName";
    private static final String BACKUP_TIME_KEY = "dailyBackupTime";
    private static final String EMAIL = "email";
    private static final String SERVER_CONNECT_INFO_KEY = "server.connectinfo";
    private static final String KEY_DATASOURCE_CLASS_KEY = "keyDataSourceClass";

    private static final String FRIEND_PREFIX = "friend.";
    private static final String EMAIL_SUFFIX = ".email";
    private static final String CONNECT_INFO_SUFFIX = ".connectinfo";
    private static final String SIGN_KEY_SUFFIX = ".signingKey";
    private static final String ENCRYPT_KEY_SUFFIX = ".encryptingKey";

    private static final String DELIM = "~";





    String myName;
    String backupPath;
    String backupStreamName;
    String computerName;
    File root;
    RemoteNodeHandle[] storingNodes;
    String restorePath;
    int localPort;
    Pair<PGPPublicKeyRingCollection, PGPSecretKeyRingCollection> keyRings;
    SecretIdentity secretIdentity;
    PublicIdentity owner;
    PGPPublicKeyRing pubkeyring;
    KeyDataSource keyDataSource;
    int backupMinute;
    int backupHour;
    private char[] passphrase;
    private RemoteNodeHandle serverAddress;
    private static String[] friends;
    File backupConfig;
    private String email;
    private Properties myProps;


    boolean dirty;

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
    public File getBackupRootDir() {
        return new File(root, backupPath);
    }

    public String getDbConnectString() {
        return "jdbc:hsqldb:file:" + getDBFile().getAbsolutePath().replace('\\', '/');
    }

    public File getDBFile() {
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

    public synchronized PGPPublicKeyRing getPublicKeyRing() throws FriendlyBackupException {
        if( this.pubkeyring == null ) {
            final String EXCEPTION_MESSAGE = "Failed to retrieve public keyring";

            PGPPublicKeyRingCollection pkrc;
            try {
                pkrc = getPublicKeyRingCollection();
                this.pubkeyring = EncryptionUtil.instance().findPublicKeyRing(pkrc, getEmail());
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
                        getPublicKeyringFile(),
                        getSecretKeyringFile(),
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

    public File getPublicKeyringFile() {
        return new File(getRoot(), "var/gnupg/pubring.gpg");
    }

    public File getSecretKeyringFile() {
        return new File(getRoot(), "var/gnupg/secring.gpg");
    }

    public File getRestoreRootDirectory() {
        return new File(getRoot(), restorePath);
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
            log.warn("Expected path {} to start with {}", path, backupRoot);
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
    
    public RemoteNodeHandle getServerAddress() {
        return serverAddress;
    }
    
    public synchronized void setMyName(String name) {
        this.myName = name;
        dirty = true;
    }
    
    public BackupConfig(File backupConfig) throws IOException, FriendlyBackupException {
        myProps = new Properties();
        this.backupConfig = backupConfig;
        
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(backupConfig));
        try {
            myProps.load(bis);
        } finally {
            bis.close();
        }

        //populate from properties
        initFromProps();

        validate();
    }

    void validate() {
        if( computerName.indexOf(BackupConfig.DELIM) != -1 ) {
            UserLog.instance().logError("Computer name may not contain {}", BackupConfig.DELIM);
            throw new RuntimeException("Computer name may not contain " + BackupConfig.DELIM);
        }
    }

    private synchronized void initFromProps() throws FriendlyBackupException {
        root = backupConfig.getParentFile().getParentFile();
        log.info("Looking for properties in {}", root);

        backupPath = getProp(BACKUP_ROOT_DIR_KEY);
        restorePath = getProp(RESTORE_ROOT_DIR_KEY);
        localPort = Integer.parseInt(getProp(LOCAL_PORT_KEY));
        backupStreamName = getProp(BACKUP_STREAM_NAME_KEY);
        computerName = getProp(COMPUTER_NAME_KEY);
        myName = getProp(MY_NAME_KEY);
        email = getProp(EMAIL);
        
        //Make sure system is ready to run
        if( "MyNickName".equals(getMyName()) ) {
            throw new FriendlyBackupException(
                    "Edit " + getRoot().getAbsolutePath() +
                    "/var/BackupConfig.properties and set the values " +
                    "appropriately (myName cannot be MyNickName).\nSee " +
                    "http://bobbymartin.name/friendlybackup/properties.html");
        }
        
        {
            String backupTime = getProp(BACKUP_TIME_KEY);
            String[] timeBits = backupTime.split(":");
            backupHour = Integer.parseInt(timeBits[0]);
            backupMinute = Integer.parseInt(timeBits[1]);
        }
        
        friends = getProp(FRIENDS_KEY).split(",");
        initStoringNodes();
        
        {
            String keyDataSourceClass = getProp(KEY_DATASOURCE_CLASS_KEY, false);
            log.info("Using keyDataSource {}", keyDataSourceClass);
            if( keyDataSourceClass == null ) {
                keyDataSource = new SwingUIKeyDataSource();
            } else {
                try {
                    Class<?> clazz = Class.forName(keyDataSourceClass);
                    keyDataSource = (KeyDataSource) clazz.newInstance();
                    keyDataSource.initFromProps(KEY_DATASOURCE_CLASS_KEY, myProps);
                } catch(Exception e) {
                    throw new FriendlyBackupException("Could not initialize key datasource: " + e.getMessage(), e);
                }
            }
        }
        
        dirty = false;
    }

    public Properties toProperties() {
        Properties retval = new Properties();

        retval.setProperty(BACKUP_ROOT_DIR_KEY, backupPath);
        retval.setProperty(RESTORE_ROOT_DIR_KEY, restorePath);
        retval.setProperty(LOCAL_PORT_KEY, ""+localPort);
        retval.setProperty(BACKUP_STREAM_NAME_KEY, backupStreamName);
        retval.setProperty(COMPUTER_NAME_KEY, computerName);
        retval.setProperty(MY_NAME_KEY, myName);
        retval.setProperty(EMAIL, email);
        retval.setProperty(BACKUP_TIME_KEY, backupHour+":"+backupMinute);
        retval.setProperty(BACKUP_STREAM_NAME_KEY, backupStreamName);
        retval.setProperty(SERVER_CONNECT_INFO_KEY, getServerAddress().getConnectString());

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(int i = 0; i < storingNodes.length; ++i) {
            String name = storingNodes[i].getName();
            
            if( !first ) {
                sb.append(',');
            }
            first = false;
            sb.append(name);
            
            retval.setProperty(FRIEND_PREFIX+name+EMAIL_SUFFIX, storingNodes[i].getEmail());
            retval.setProperty(FRIEND_PREFIX+name+CONNECT_INFO_SUFFIX, storingNodes[i].getConnectString());
        }
        
        retval.setProperty(FRIENDS_KEY, sb.toString());
        
        return retval;
    }

    public synchronized void save() throws IOException {
        if( dirty ) {
            doSave();
        }
    }
    
    private synchronized void doSave() throws IOException {
        Properties props = toProperties();
        
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(backupConfig));
        try {
            props.store(out, "Automatically generated by BackupConfig.doSave");
        } finally {
            out.close();
        }

        dirty = false;
    }

    private void initStoringNodes() throws FriendlyBackupException {
        if( storingNodes == null ) {
            String serverConnectInfo = getProp(SERVER_CONNECT_INFO_KEY);
            serverAddress = new RemoteNodeHandle(
                    SERVER_NAME,
                    SERVER_EMAIL,
                    serverConnectInfo,
                    //TODO use real identity handle for server
                    new PublicIdentityHandle(0, 0)
                    );

            storingNodes = new RemoteNodeHandle[friends.length];
            for(int i = 0; i < friends.length; ++i) {
                String email = getProp(FRIEND_PREFIX+friends[i]+EMAIL_SUFFIX);
                String connectInfo = getProp(FRIEND_PREFIX+friends[i]+CONNECT_INFO_SUFFIX);

                PublicIdentityHandle pih = getFriendPublicIdentityHandle(email);
                storingNodes[i] = new RemoteNodeHandle(
                        friends[i],
                        email,
                        connectInfo,
                        pih
                        );
            }
        }
    }

    public PublicIdentityHandle getFriendPublicIdentityHandle(String friend)
            throws FriendlyBackupException {
        String signKeyProp = FRIEND_PREFIX+friend+SIGN_KEY_SUFFIX;
        String encryptKeyProp = FRIEND_PREFIX+friend+ENCRYPT_KEY_SUFFIX;

        PublicIdentityHandle pih = null;
        if( propExists(signKeyProp) && propExists(encryptKeyProp) ) {
            long signKeyId = getLongProp(signKeyProp);
            long encryptKeyId = getLongProp(encryptKeyProp);
            pih = new PublicIdentityHandle(encryptKeyId, signKeyId);
        } else {
            pih = lookupFriendPublicIdentityHandle(friend);
        }

        return pih;
    }

    private PublicIdentityHandle lookupFriendPublicIdentityHandle(String friend)
            throws FriendlyBackupException {
        try {
            PGPPublicKeyRing pubKeyRing = EncryptionUtil.instance()
                    .findPublicKeyRing(getPublicKeyRingCollection(), friend);

            PGPPublicKey signingKey = EncryptionUtil.instance()
                    .findFirstSigningKey(pubKeyRing);

            PGPPublicKey pubEncryptingKey = EncryptionUtil.instance()
                    .findFirstEncryptingKey(pubKeyRing);

            return new PublicIdentityHandle(signingKey, pubEncryptingKey);
        } catch (PGPException e) {
            throw new FriendlyBackupException(
                    "Could not find encrypting and/or signing key for friend "
                            + friend, e);
        } catch (IOException e) {
            throw new FriendlyBackupException(
                    "Could not find encrypting and/or signing key for friend "
                            + friend, e);
        }
    }
      
    private boolean propExists(String propName) {
        return myProps.getProperty(propName) != null;
    }

    private long getLongProp(String propName) throws NumberFormatException, FriendlyBackupException {
        return Long.valueOf(getProp(propName));
    }


    public void setStoringNodes(RemoteNodeHandle[] newStoringNodes) {
        storingNodes = newStoringNodes;
    }

    
    private String getProp(String propName) throws FriendlyBackupException {
        return getProp(propName, true);
    }
    
    private String getProp(String propName, boolean required) throws FriendlyBackupException {
        String retval = myProps.getProperty(propName);
        if( required && retval == null ) {
            throw new FriendlyBackupException("It looks as if " + propName + " is missing from BackupConfig.properties");
        }
        return retval;
    }
    
    public boolean equals(Object obj) {
        if( obj instanceof BackupConfig ) {
            BackupConfig rhs = (BackupConfig) obj;
            
            boolean retval = true;
            retval = retval && ObjectUtil.equals(backupPath, rhs.backupPath);
            retval = retval && ObjectUtil.equals(restorePath, rhs.restorePath);
            retval = retval && ObjectUtil.equals(backupStreamName, rhs.backupStreamName);
            retval = retval && ObjectUtil.equals(computerName, rhs.computerName);
            retval = retval && ObjectUtil.equals(myName, rhs.myName);
            retval = retval && ObjectUtil.equals(backupStreamName, rhs.backupStreamName);
            retval = retval && ObjectUtil.equals(backupPath, rhs.backupPath);
            retval = retval && (localPort == rhs.localPort);
            retval = retval && (backupHour == rhs.backupHour);
            retval = retval && (backupMinute == rhs.backupMinute);
            retval = retval && Arrays.equals(storingNodes, rhs.storingNodes);
            
            return retval;
        } else {
            return false;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("BackupConfig(");
        sb.append("backupPath: ").append(backupPath);
        sb.append(", restorePath: ").append(restorePath);
        sb.append(", backupStreamName: ").append(backupStreamName);
        sb.append(", computerName: ").append(computerName);
        sb.append(", myName: ").append(myName);
        sb.append(", backupStreamName: ").append(backupStreamName);
        sb.append(", backupPath: ").append(backupPath);
        sb.append(", localPort: ").append(localPort);
        sb.append(", backupHour: ").append(backupHour);
        sb.append(", backupMinute: ").append(backupMinute);
        sb.append(", storingNodes: ").append(Arrays.toString(storingNodes));
        sb.append(")");
        return sb.toString();
    }
    
    public synchronized void setEmail(String email) {
        this.email = email;
        dirty = true;
    }

    public synchronized String getEmail() {
        return this.email;
    }
}
