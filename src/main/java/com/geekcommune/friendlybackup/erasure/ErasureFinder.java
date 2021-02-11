package com.geekcommune.friendlybackup.erasure;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.onionnetworks.util.Buffer;

public interface ErasureFinder {
    public Buffer getErasure(int idx) throws FriendlyBackupException;
}
