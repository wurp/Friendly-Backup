package com.geekcommune.identity;

import org.bouncycastle.openpgp.PGPPublicKey;

import com.geekcommune.friendlybackup.format.Data;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.util.StringUtil;
import com.google.protobuf.ByteString;

/**
 * A small unique identifier for a given public key (and implicitly the secret key as well).
 * In either case, this identifier is only useful if you already have the public or secret
 * key available.
 * @author bobbym
 *
 */
public class PublicIdentityHandle implements Data<Basic.PublicIdentityHandle>{

    private byte[] fingerprint;

    public PublicIdentityHandle(PGPPublicKey key) {
        this.fingerprint = key.getFingerprint();
    }

    protected PublicIdentityHandle() {
    }

    public Basic.PublicIdentityHandle toProto() {
        Basic.PublicIdentityHandle.Builder bldr = Basic.PublicIdentityHandle.newBuilder();
        bldr.setVersion(1);
        bldr.setFingerprint(ByteString.copyFrom(fingerprint));
        return bldr.build();
    }

    public static PublicIdentityHandle fromProto(
            com.geekcommune.friendlybackup.proto.Basic.PublicIdentityHandle ownerHandle) {
        PublicIdentityHandle retval = new PublicIdentityHandle();
        retval.fingerprint = ownerHandle.getFingerprint().toByteArray();
        return retval;
    }

    public String fingerprintString() {
        return StringUtil.hexdump(fingerprint);
    }

}
