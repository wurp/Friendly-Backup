package com.geekcommune.identity;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.Data;
import com.geekcommune.friendlybackup.proto.Basic;

/**
 * A small unique identifier for a given public key (and implicitly the secret key as well).
 * In either case, this identifier is only useful if you already have the public or secret
 * key available.
 * @author bobbym
 *
 */
public class PublicIdentityHandle implements Data<Basic.PublicIdentityHandle>{

    private long encryptingKeyID;
    private long signingKeyID;

//    public PublicIdentityHandle(PGPPublicKey key) {
//        this(key.getFingerprint());
//    }

//    public PublicIdentityHandle(byte[] fingerprint) {
//        this.fingerprint = fingerprint;
//    }

    protected PublicIdentityHandle() {
    }

    public PublicIdentityHandle(
            PGPPublicKey encryptingKey,
            PGPPublicKey signingKey) {
        this.encryptingKeyID = encryptingKey == null ? 0 : encryptingKey.getKeyID();
        this.signingKeyID = signingKey == null ? 0 : signingKey.getKeyID();
    }

    public PublicIdentityHandle(
            long encryptingKeyID,
            long signingKeyID) {
        this.encryptingKeyID = encryptingKeyID;
        this.signingKeyID = signingKeyID;
    }

    public Basic.PublicIdentityHandle toProto() {
        Basic.PublicIdentityHandle.Builder bldr = Basic.PublicIdentityHandle.newBuilder();
        bldr.setVersion(1);
        bldr.setEncryptingKeyID(encryptingKeyID);
        bldr.setSigningKeyID(signingKeyID);
        return bldr.build();
    }

    public static PublicIdentityHandle fromProto(
            com.geekcommune.friendlybackup.proto.Basic.PublicIdentityHandle ownerHandle) {
        PublicIdentityHandle retval = new PublicIdentityHandle();
        retval.encryptingKeyID = ownerHandle.getEncryptingKeyID();
        retval.signingKeyID = ownerHandle.getSigningKeyID();
        return retval;
    }

    public PGPPublicKey findEncryptingKey(PGPPublicKeyRing pubKeyRing) {
        if( encryptingKeyID == 0 ) {
            return null;
        } else {
            return pubKeyRing.getPublicKey(encryptingKeyID);
        }
    }

    public PGPPublicKey findSigningKey(PGPPublicKeyRing pubKeyRing) {
        if( signingKeyID == 0 ) {
            return null;
        } else {
            return pubKeyRing.getPublicKey(signingKeyID);
        }
    }

    public long getSigningKeyID() {
        return signingKeyID;
    }

    public PGPPublicKey findEncryptingKey(PGPPublicKeyRingCollection keyRings) throws FriendlyBackupException {
        final String exceptionMessage = "Could not find encrypting key";
        try {
            if (encryptingKeyID == 0) {
                return null;
            } else {
                return keyRings.getPublicKey(encryptingKeyID);
            }
        } catch (PGPException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        }
    }

    public PGPPublicKey findSigningKey(PGPPublicKeyRingCollection keyRings) throws FriendlyBackupException {
        final String exceptionMessage = "Could not find signing key";
        try {
            if( signingKeyID == 0 ) {
                return null;
            } else {
                return keyRings.getPublicKey(signingKeyID);
            }
        } catch (PGPException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        }
    }

}
