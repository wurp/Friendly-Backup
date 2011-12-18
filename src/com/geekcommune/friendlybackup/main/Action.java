package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;

import com.geekcommune.friendlybackup.config.BackupConfig;

public class Action {
	public static final String BACKUP_CONFIG_PROP_KEY = "BackupConfigFile";
	public static final int MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

	protected BackupConfig getBackupConfig() throws IOException {
		File cfgFile = new File(System.getProperty(BACKUP_CONFIG_PROP_KEY));
		BackupConfig bakcfg = BackupConfig.parseConfigFile(cfgFile);
		return bakcfg;
	}

}
