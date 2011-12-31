package com.geekcommune.identity;

import org.bouncycastle.openpgp.PGPPublicKey;


public class PublicIdentity {

    private PGPPublicKey key;
    
    public PublicIdentity(PGPPublicKey publicKey) {
        this.key = publicKey;
    }

    public PublicIdentityHandle getHandle() {
        return new PublicIdentityHandle(key);
    }

}
