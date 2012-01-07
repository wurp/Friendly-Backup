package com.geekcommune.friendlybackup.logging;

import org.apache.log4j.Logger;

public class LoggingUserLog extends UserLog {
    private static final Logger log = Logger.getLogger(LoggingUserLog.class);
    private static final Logger userlog = Logger.getLogger("UserLog");

    @Override
    public void logError(String message, Exception e) {
        userlog.error(message);
        log.error(message + ": " + e.getMessage(), e);
    }

    @Override
    public void logError(String message) {
        userlog.error(message);
        log.error(message);
    }

    @Override
    public void logInfo(String message) {
        userlog.info(message);
        log.info(message);
    }

}
