package com.geekcommune.friendlybackup.logging;

import org.apache.log4j.Logger;

public class LoggingUserLog extends UserLog {
    private static final Logger log = Logger.getLogger(LoggingUserLog.class);

    @Override
    public void logError(String message, Exception e) {
        log.error(message, e);
    }

    @Override
    public void logError(String message) {
        log.error(message);
    }

}