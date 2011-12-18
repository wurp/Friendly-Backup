package com.geekcommune.friendlybackup.communication;

import com.geekcommune.communication.MessageUtil;
import com.geekcommune.friendlybackup.main.ProgressTracker;

public class BackupMessageUtil extends MessageUtil {

    private static BackupMessageUtil instance;

    public static BackupMessageUtil instance() {
        // TODO Auto-generated method stub
        return instance;
    }

    public static void setInstance(BackupMessageUtil instance) {
        BackupMessageUtil.instance = instance;
    }

    public void processBackupMessages(ProgressTracker progressTracker) {
        // TODO Auto-generated method stub
        
    }

    public void cleanOutBackupMessageQueue() {
        // TODO Auto-generated method stub
        
    }
}
