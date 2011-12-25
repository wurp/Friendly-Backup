package com.geekcommune.identity;

import java.util.Date;

import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;


public class PrivateIdentity {
    public PrivateIdentity() {
        
    }
    
	public PublicIdentity getPublicIdentity() {
		// TODO Auto-generated method stub
		return new PublicIdentity();
	}

	public Signature sign(byte[] data) {
	    //TODO BOBBY
		return Signature.Dummy;
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
