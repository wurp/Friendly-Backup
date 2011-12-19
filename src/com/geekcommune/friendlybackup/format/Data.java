package com.geekcommune.friendlybackup.format;

import com.google.protobuf.AbstractMessage;

/**
 * Marker interface for things that we retrieve from the datastore.
 * FBType is the FriendlyBackupType; it is the concrete subtype we implement in Friendly Backup.
 * @author bobbym
 *
 */
public interface Data<ProtoType extends AbstractMessage> {
    ProtoType toProto();
}
