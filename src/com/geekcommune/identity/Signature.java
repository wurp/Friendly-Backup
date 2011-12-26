package com.geekcommune.identity;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSignature;

import com.geekcommune.friendlybackup.format.Data;
import com.geekcommune.friendlybackup.proto.Basic;
import com.google.protobuf.ByteString;

public class Signature implements Data<Basic.Signature>{
    public static final Signature Dummy = new Signature();

    /**
     * This is a special signature that should ONLY EVER be used for data completely generated
     * within the app.  Never accept this signature from an incoming message, or send it to another node.
     */
    public static final Signature INTERNAL_SELF_SIGNED = new Signature();
    
    private PGPSignature pgpSignature;

    public Signature() {
        // TODO Auto-generated constructor stub
    }

    public Signature(String sigString) {
        // TODO Auto-generated constructor stub
    }

    public Basic.Signature toProto() {
        Basic.Signature.Builder bldr = Basic.Signature.newBuilder();
        bldr.setVersion(1);

        //TODO ensure we never send INTERNAL_SELF_SIGNED - maybe silently transform into real self-signature?
//        try {
            //TODO BOBBY
            bldr.setSignature(ByteString.copyFrom(new byte[0]));
//        } catch (PGPException e) {
//            throw new RuntimeException(e);
//        }

        return bldr.build();
    }

    public static Signature fromProto(Basic.Signature signature) {
        //TODO ensure we never accept INTERNAL_SELF_SIGNED - transform into another invalid signature
        //TODO BOBBY
        return Signature.Dummy;
    }
}
