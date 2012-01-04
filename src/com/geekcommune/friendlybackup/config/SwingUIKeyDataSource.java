package com.geekcommune.friendlybackup.config;

import com.geekcommune.identity.KeyDataSource;

public class SwingUIKeyDataSource implements KeyDataSource {
    private char[] passphrase;

    public SwingUIKeyDataSource() {
    }

    public void setPassphrase(char[] passphrase) {
        this.passphrase = passphrase;
    }
    
    public char[] getPassphrase() {
        if( passphrase == null ) {
            SwingPasswordDialog dlg = new SwingPasswordDialog();
            passphrase = dlg.getPassword();
        }
        return passphrase;
    }

    public String getIdentity() {
        // TODO Auto-generated method stub
        return null;
    }

}
