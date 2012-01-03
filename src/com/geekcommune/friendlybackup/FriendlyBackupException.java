package com.geekcommune.friendlybackup;


public class FriendlyBackupException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public FriendlyBackupException(String message, Exception e) {
        super(message, e);
    }

    public FriendlyBackupException(String message) {
        super(message);
    }

}
