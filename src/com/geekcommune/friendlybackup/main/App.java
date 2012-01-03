package com.geekcommune.friendlybackup.main;

import java.io.File;
import java.io.IOException;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.communication.message.AbstractMessage;
import com.geekcommune.friendlybackup.communication.BackupMessageUtil;
import com.geekcommune.friendlybackup.communication.message.RetrieveDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendErasureMessage;
import com.geekcommune.friendlybackup.config.BackupConfig;
import com.geekcommune.friendlybackup.config.SwingUIKeyDataSource;
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
            bakcfg.setKeyDataSource(new SwingUIKeyDataSource());

            //DataStore
            DataStore.setInstance(new InMemoryDataStore());
            
            //BackupMessageUtil
            BackupMessageUtil.setInstance(new BackupMessageUtil());
            BackupMessageUtil.instance().setBackupConfig(bakcfg);
            MessageUtil.setInstance(BackupMessageUtil.instance());
            BackupMessageUtil.instance().startListenThread();
            
            //set up messages to be read from the input data
            AbstractMessage.registerMessageFactory(VerifyMaybeSendDataMessage.INT_TYPE, VerifyMaybeSendDataMessage.FACTORY);
            AbstractMessage.registerMessageFactory(RetrieveDataMessage.INT_TYPE, RetrieveDataMessage.FACTORY);
            AbstractMessage.registerMessageFactory(VerifyMaybeSendErasureMessage.INT_TYPE, VerifyMaybeSendErasureMessage.FACTORY);
        }
    }


    public static BackupConfig getBackupConfig() {
        return bakcfg;
    }

}
