package com.geekcommune.friendlybackup.main;

import java.io.IOException;

import org.bouncycastle.openpgp.PGPException;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.config.SwingCreateAccountDialog;
import com.geekcommune.friendlybackup.config.SwingUIKeyDataSource;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.identity.EncryptionUtil;

public class FBNodeImpl {
	// TODO make configurable?
    private static final int MAX_PASSWORD_RETRIES = 5;
    
	private Backup backup;
    private Restore restore;
    private BackupConfig bakcfg;

    public FBNodeImpl(BackupConfig backupConfig) throws IOException {
        this.bakcfg = backupConfig;
        backup = new Backup(this.bakcfg);
        restore = new Restore(this.bakcfg);
    }

    public void updateServer() throws FriendlyBackupException, IOException {
    	// TODO bobby I don't remember what I was doing with the server exactly :-O
//        ClientUpdate cu = new ClientUpdate(
//                getBackupConfig().getMyName(),
//                getBackupConfig().getEmail(),
//                getBackupConfig().getRoot().getFreeSpace(),
//                getBackupConfig().getPublicKeyRing().getEncoded(),
//                getBackupConfig().getAuthenticatedOwner());
//
//        ClientStartupMessage csm = new ClientStartupMessage(
//                getBackupConfig().getServerAddress(),
//                getBackupConfig().getLocalPort(),
//                cu);
//
//        BackupMessageUtil.instance().queueMessage(csm);
//        
//        //block for the response
//        try {
//            if( !csm.awaitResponse(30000) ) {
//                throw new FriendlyBackupException("Timed out waiting for " + getBackupConfig().getServerAddress());
//            }
//        } catch (InterruptedException e) {
//            throw new FriendlyBackupException("Interrupted while waiting for response from server", e);
//        }
//        
//        ConfirmationMessage confirmationMsg = csm.getConfirmation();
//        if( !confirmationMsg.isOK() ) {
//            throw new FriendlyBackupException("Could not register with server: " +
//                    confirmationMsg.getErrorMessage());
//        }
    }

    public void createKeyringIfNeeded() throws IOException,
            InterruptedException {
        if( !getBackupConfig().getSecretKeyringFile().isFile() ) {
        	// TODO make this work for non-interactive key data sources
            SwingCreateAccountDialog createAccountDialog = new SwingCreateAccountDialog();

            if( createAccountDialog.getPassphrase() == null ) {
                UserLog.instance().logInfo("Exiting at user's request");
                Thread.sleep(100);
                System.exit(-1);
            }

            String name = createAccountDialog.getName();
            String email = createAccountDialog.getEmail();
            char[] passphrase = createAccountDialog.getPassphrase();
            
            getBackupConfig().setMyName(name);
            getBackupConfig().setEmail(email);
            ((SwingUIKeyDataSource)getBackupConfig().getKeyDataSource()).setPassphrase(passphrase);
            
            EncryptionUtil.instance().generateKey(
                    name,
                    email,
                    passphrase,
                    getBackupConfig().getPublicKeyringFile(),
                    getBackupConfig().getSecretKeyringFile());
        }
    }

    public void authenticateUser(char[] passphrase) throws FriendlyBackupException {
        boolean passphraseCorrect = false;
        int badPasswordCount = 0;
        while(!passphraseCorrect) {
            //make sure we have the passphrase now, since the user is presumably at the computer
            //Empty passphrase is interpreted as meaning "quit"
            if( getBackupConfig().getKeyDataSource().getPassphrase() == null ) {
                UserLog.instance().logInfo("Exiting at user's request");
                try { Thread.sleep(100); } catch(InterruptedException e) {}
                System.exit(-1);
            }
            
            //verify that the passphrase is correct
            try {
                getBackupConfig().getAuthenticatedOwner().sign(new byte[] { 1, 2, 3, 4, 5 });
                passphraseCorrect = true;
            } catch (FriendlyBackupException e) {
                UserLog.instance().logError("pwd error", e);
                if( e.getCause() instanceof PGPException ) {
                	if (badPasswordCount < MAX_PASSWORD_RETRIES) {
                    	++badPasswordCount;
                        getBackupConfig().getKeyDataSource().clearPassphrase();
                	} else {
                        throw e;
                	}
                } else {
                    throw e;
                }
            }
        }
    }
    
    public void backup() throws IOException, InterruptedException {
        backup.doBackup();
    }

    public void restore() throws FriendlyBackupException, InterruptedException {
        restore.doRestore();
    }


    public BackupConfig getBackupConfig() {
        return bakcfg;
    }
}
