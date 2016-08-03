package com.geekcommune.friendlybackup.config;

import java.util.Properties;

import com.geekcommune.identity.KeyDataSource;

public class PrepopulatedKeyDataSource implements KeyDataSource {
	private String identity;
	private char[] passphrase;

	public PrepopulatedKeyDataSource() {
		
	}
	
	public PrepopulatedKeyDataSource(String identity, char[] passphrase) {
		this.identity = identity;
		this.passphrase = passphrase;
	}
	
	@Override
	public void clearPassphrase() {
		//does nothing - no way to repopulate passphrase
	}

	@Override
	public String getIdentity() {
		return identity;
	}

	@Override
	public char[] getPassphrase() {
		return passphrase;
	}

	@Override
	public void initFromProps(String propNamePrefix, Properties props) {
		identity = props.getProperty(propNamePrefix + ".identity");
		passphrase = props.getProperty(propNamePrefix + ".passphrase").toCharArray();
	}

}
