package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;

import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.datastore.DataStore;
import com.geekcommune.friendlybackup.datastore.InMemoryDataStore;
import com.geekcommune.friendlybackup.logging.LoggingUserLog;
import com.geekcommune.friendlybackup.logging.UserLog;

public class App {
    
    public static final String BACKUP_CONFIG_PROP_KEY = "BackupConfigFile";
    private static boolean wired = false;
    private static BackupConfig bakcfg;

    /**
     * Dependency Injection
     * @throws IOException 
     */
    public static synchronized void wire() throws IOException {
        wire(System.getProperty(BACKUP_CONFIG_PROP_KEY));
    }

    /**
     * Dependency Injection
     * @throws IOException 
     */
    public static synchronized void wire(String configFilePath) throws IOException {
        if( !wired ) {
            wired = true;

            //UserLog
            UserLog.setInstance(new LoggingUserLog());

            //BackupConfig
            File cfgFile = new File(configFilePath);
            bakcfg = BackupConfig.parseConfigFile(cfgFile);

            //DataStore
            DataStore.setInstance(new InMemoryDataStore());
            
            //BackupMessageUtil
            BackupMessageUtil.setInstance(new BackupMessageUtil());
            BackupMessageUtil.instance().setBackupConfig(bakcfg);
        }
    }


    public static BackupConfig getBackupConfig() {
        return bakcfg;
    }

}
