package com.geekcommune.friendlybackup.format.low;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.util.StringUtil;
import com.google.protobuf.ByteString;

/**
 * aka HashID
 * 
 * 16 byte identifier for low-level objects.  Usually the 160 bit SHA1 hash of the whole data content.
 * 
 * @author bobbym
 *
 */
public class HashIdentifier extends BaseData<Basic.HashIdentifier> {

	//size in bytes
	public static final int SIZEOF = 20;
    public static final HashIdentifier Dummy = new HashIdentifier(new byte[20]);
    
    private byte[] digest;

	public HashIdentifier(byte[] digest) {
	    if( digest.length != SIZEOF ) {
	        throw new RuntimeException("Hash identifiers must be of length 20, was " + digest.length);
	    }
	    
	    this.digest = digest;
    }

    public byte[] getData() {
		return digest;
	}

    public static HashIdentifier hashForBytes(byte[] data) {
        MessageDigest md = null;
        try {
          md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException("No SHA support!",e);
        }

        md.update(data);
        
        return new HashIdentifier(md.digest());
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof HashIdentifier ) {
            return Arrays.equals(((HashIdentifier)obj).getData(), getData());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getData());
    }
    
    public Basic.HashIdentifier toProto() {
        Basic.HashIdentifier.Builder bldrId = Basic.HashIdentifier.newBuilder();
        
        bldrId.setVersion(1);
        bldrId.setData(ByteString.copyFrom(getData()));
        bldrId.setEncoding(Basic.HashIdentifier.HashEncoding.SHA1_160_BITS);
        
        return bldrId.build();
    }

    public static HashIdentifier fromProto(
            Basic.HashIdentifier proto) {
        versionCheck(1, proto.getVersion(), proto);
        
        HashIdentifier retval = new HashIdentifier(proto.getData().toByteArray());
        
        return retval;
    }
    
    @Override
    public String toString() {
        //only send a fingerprint of the data
        return StringUtil.hexdump(this.digest, this.digest.length - 4, 4);
    }
}
