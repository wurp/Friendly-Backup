package com.geekcommune.identity;

import com.geekcommune.friendlybackup.format.Data;
import com.geekcommune.friendlybackup.proto.Basic;
import com.google.protobuf.ByteString;

public class PublicIdentityHandle implements Data<Basic.PublicIdentityHandle>{

    public Basic.PublicIdentityHandle toProto() {
        Basic.PublicIdentityHandle.Builder bldr = Basic.PublicIdentityHandle.newBuilder();
        //TODO BOBBY
        bldr.setVersion(0);
        bldr.setFingerprint(ByteString.copyFrom(new byte[0]));
        return bldr.build();
    }

    public static PublicIdentityHandle fromProto(
            com.geekcommune.friendlybackup.proto.Basic.PublicIdentityHandle ownerHandle) {
        //TODO BOBBY
        return new PublicIdentityHandle();
    }

}
