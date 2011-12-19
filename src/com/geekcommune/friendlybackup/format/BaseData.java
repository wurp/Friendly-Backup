package com.geekcommune.friendlybackup.format;

import com.geekcommune.friendlybackup.erasurefinder.UserLog;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.google.protobuf.AbstractMessage;

public abstract class BaseData<ProtoType extends AbstractMessage> implements Data<ProtoType> {
    protected static void versionCheck(int maxExpectedVersion, int actualVersion, Object proto) {
        String instanceName = proto.getClass().getName();
        
        if( actualVersion > maxExpectedVersion ) {
            RuntimeException e = new RuntimeException("Unknown version " + actualVersion + " of " + instanceName);
            UserLog.instance().logError("Software out of date.  Please upgrade Friendly Backup before restoring this backup.", e);
            throw e;
        }
        
        if( actualVersion == 0 ) {
            RuntimeException e = new RuntimeException("Proto from unimplemented version of " + instanceName);
            UserLog.instance().logError("Unsupported data type encountered.  Backup/restore may be incomplete.", e);
            throw e;
        }
        
        if( actualVersion < 0 ) {
            RuntimeException e = new RuntimeException("Bad data encountered when parsing version of " + instanceName);
            UserLog.instance().logError("Corrupt data encountered.  Backup/restore may be incomplete.", e);
            throw e;
        }
    }

    public byte[] getData() {
        return toProto().toByteArray();
    }

    public HashIdentifier getHashID() {
        return HashIdentifier.hashForBytes(getData());
    }
}
