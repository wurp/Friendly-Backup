package com.geekcommune.friendlybackup.format.high;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.proto.Basic;
import com.google.protobuf.InvalidProtocolBufferException;

public class BackupManifest extends BaseData<Basic.BackupManifest>{

	private String backupStreamName;
    private Date backupDate;
    private List<HashIdentifier> backupFileLabelIds = new ArrayList<HashIdentifier>();

    public BackupManifest(String backupStreamName, Date backupDate) {
		this.backupStreamName = backupStreamName;
		this.backupDate = backupDate;
	}

	public void add(HashIdentifier labelIdOfBackupFile) {
		backupFileLabelIds.add(labelIdOfBackupFile);
	}

    public List<HashIdentifier> getFileLabelIDs() {
        return Collections.unmodifiableList(backupFileLabelIds);
    }

    public static BackupManifest fromProto(Basic.BackupManifest proto) throws InvalidProtocolBufferException {
        versionCheck(1, proto.getVersion(), proto);
        
        BackupManifest retval = new BackupManifest(
                proto.getBackupStreamName(),
                new Date(proto.getBackupTimestampMillis()));
        for(Basic.HashIdentifier fileLabelId : proto.getBackupFileLabelIdsList()) {
            retval.add(HashIdentifier.fromProto(fileLabelId));
        }
        
        return retval;
    }

    public Basic.BackupManifest toProto() {
        Basic.BackupManifest.Builder bldr = Basic.BackupManifest.newBuilder();
        bldr.setVersion(1);
        bldr.setBackupStreamName(backupStreamName);
        bldr.setBackupTimestampMillis(backupDate.getTime());
        
        for(HashIdentifier fileLabelId : backupFileLabelIds) {
            bldr.addBackupFileLabelIds(fileLabelId.toProto());
        }
        
        return bldr.build();
    }

}
