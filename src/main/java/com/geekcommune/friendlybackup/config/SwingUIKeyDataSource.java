package com.geekcommune.friendlybackup.config;

import java.util.Properties;

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
            SwingPassphraseDialog dlg = new SwingPassphraseDialog();
            passphrase = dlg.getPassphrase();
        }
        return passphrase;
    }

    public String getIdentity() {
        // TODO Auto-generated method stub
        return null;
    }

    public void clearPassphrase() {
        passphrase = null;
    }

	@Override
	public void initFromProps(String propNamePrefix, Properties props) {
		//nothing to do
	}

}
