package com.geekcommune.identity;


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

}
