package com.geekcommune.identity;

import java.util.Date;

import org.bouncycastle.openpgp.PGPSecretKey;

import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;


public class SecretIdentity {
    PGPSecretKey key;
    
    protected SecretIdentity() {
    }
    
	public PublicIdentity getPublicIdentity() {
		return new PublicIdentity(key.getPublicKey());
	}

	public Signature sign(byte[] data) {
		return null; //new Signature(EncryptionUtil.instance().makeSignature(data, key.getPublicKey(), key.extractPrivateKey(passPhrase, "BC")));
	}

	/**
	 * Create and sign a lease for a piece of data
	 * 
	 * @param expiryDate
	 * @return
	 */
    public Lease makeLease(HashIdentifier id, Date expiryDate) {
        return new Lease(
                expiryDate,
                getPublicIdentity().getHandle(),
                Signature.Dummy);
    }

}
