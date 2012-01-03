package com.geekcommune.identity;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;

import com.geekcommune.friendlybackup.FriendlyBackupException;


public class PublicIdentity {

    private PGPPublicKey encryptingKey;
    private PGPPublicKey signingKey;

    public PublicIdentity(PGPPublicKeyRing pubKeyRing,
            PublicIdentityHandle publicIdentityHandle) throws FriendlyBackupException {
        this.encryptingKey = publicIdentityHandle.findEncryptingKey(pubKeyRing);
        this.signingKey = publicIdentityHandle.findSigningKey(pubKeyRing);
    }

    public PublicIdentity(
            PGPPublicKeyRingCollection keyRings,
            PublicIdentityHandle publicIdentityHandle)
                    throws FriendlyBackupException {
        this.encryptingKey = publicIdentityHandle.findEncryptingKey(keyRings);
        this.signingKey = publicIdentityHandle.findSigningKey(keyRings);
    }

    public PublicIdentityHandle getHandle() {
        return new PublicIdentityHandle(encryptingKey, signingKey);
    }

    public PGPPublicKey getPGPPublicKey() {
        return encryptingKey;
    }

}
