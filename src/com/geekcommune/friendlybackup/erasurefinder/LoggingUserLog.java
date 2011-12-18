package com.geekcommune.friendlybackup.erasurefinder;

import org.apache.log4j.Logger;

public class LoggingUserLog extends UserLog {
    private static final Logger log = Logger.getLogger(LoggingUserLog.class);

    @Override
    public void logError(String string, Exception e) {
        log.error(string, e);
    }

}
