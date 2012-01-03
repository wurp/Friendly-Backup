package com.geekcommune.identity;

import java.io.IOException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.logging.UserLog;
import com.geekcommune.friendlybackup.proto.Basic;
import com.google.protobuf.ByteString;

public class Signature extends BaseData<Basic.Signature>{
    /**
     * This is a special signature that should ONLY EVER be used for data completely generated
     * within the app.  Never accept this signature from an incoming message, or send it to another node.
     */
    public static final Signature INTERNAL_SELF_SIGNED = new Signature();
    
    private PGPSignature pgpSignature;

    protected Signature() {
    }

    public Signature(PGPSignature pgpSig) {
        this.pgpSignature = pgpSig;
    }

    public Basic.Signature toProto() {
        Basic.Signature.Builder bldr = Basic.Signature.newBuilder();
        bldr.setVersion(1);

        try {
            //we stream INTERNAL_SELF_SIGNED so we can get a byte array to sign, but
            //it should never actually be pulled that way in fromProto...
            byte[] sigBytes = pgpSignature == null ? new byte[0] :
                pgpSignature.getEncoded();
            bldr.setSignature(ByteString.copyFrom(sigBytes));
        } catch (IOException e) {
            UserLog.instance().logError("Some kind of coding error recovering signature - this should never happen", e);
        }

        return bldr.build();
    }

    public static Signature fromProto(Basic.Signature proto) throws FriendlyBackupException {
        //TODO ensure we never accept INTERNAL_SELF_SIGNED - transform into another invalid signature
        versionCheck(1, proto.getVersion(), proto);
        
        byte[] data = proto.getSignature().toByteArray();
        
        if( data == null ) {
            throw new FriendlyBackupException("Attempt to build from bogus internal signature");
        }

        final String exceptionMessage = "Failed to recover Signature from bytes";
        try {
            return new Signature(PGPSignatureCheat.newPGPSignature(data));
        } catch (IOException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        }
    }

    public boolean verify(PublicIdentity pubIdent, byte[] bytesToSign) throws FriendlyBackupException {
        final String exceptionMessage = "Could not verify signature";
        
        try {
            PGPPublicKey pgpPublicKey = pubIdent.getPGPPublicKey();
            pgpSignature.initVerify(pgpPublicKey, "BC");
            
            for(int i = 0; i < bytesToSign.length; ++i) {
                pgpSignature.update(bytesToSign[i]);
            }
            
            return pgpSignature.verify();
        } catch (NoSuchProviderException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (PGPException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (SignatureException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        }
    }
}
