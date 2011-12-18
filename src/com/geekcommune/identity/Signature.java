package com.geekcommune.identity;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSignature;

import com.geekcommune.friendlybackup.format.Data;
import com.geekcommune.friendlybackup.proto.Basic;
import com.google.protobuf.ByteString;

public class Signature implements Data<Basic.Signature>{
    public static final Signature Dummy = new Signature();
    
    private PGPSignature pgpSignature;

    public Basic.Signature toProto() {
        Basic.Signature.Builder bldr = Basic.Signature.newBuilder();
        bldr.setVersion(1);

//        try {
            //TODO BOBBY
            bldr.setSignature(ByteString.copyFrom(new byte[0]));
//        } catch (PGPException e) {
//            throw new RuntimeException(e);
//        }

        return bldr.build();
    }

    public static Signature fromProto(Basic.Signature signature) {
        //TODO BOBBY
        return Signature.Dummy;
    }
}
