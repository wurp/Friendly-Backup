package com.geekcommune.friendlybackup.erasurefinder;

public abstract class UserLog {

	public abstract void logError(String message, Exception e);

    public abstract void logError(String message);

	protected static UserLog instance;
	
    public static UserLog instance() {
        return instance;
    }

    public static void setInstance(UserLog userLog) {
        instance = userLog;
    }

}
