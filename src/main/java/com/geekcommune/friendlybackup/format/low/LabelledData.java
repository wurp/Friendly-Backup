package com.geekcommune.friendlybackup.format.low;

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.identity.PublicIdentity;
import com.geekcommune.identity.SecretIdentity;
import com.geekcommune.identity.PublicIdentityHandle;
import com.geekcommune.identity.Signature;
import com.google.protobuf.ByteString;

public class LabelledData extends BaseData<Basic.LabelledData> {

    private PublicIdentityHandle owner;
    private Signature signature;
    private HashIdentifier pointingAt;
    private byte[] encryptedLabel;

    public LabelledData(
            SecretIdentity owner,
            byte[] encryptedLabel,
            HashIdentifier pointingAt)
                    throws FriendlyBackupException {
        this.encryptedLabel = encryptedLabel;
        this.owner = owner.getPublicIdentity().getHandle();
        this.pointingAt = pointingAt;
        this.sign(owner);
    }

    protected LabelledData(
            byte[] encryptedLabel,
            PublicIdentityHandle ownerHandle,
            Signature signature,
            HashIdentifier pointingAt) {
        this.owner = ownerHandle;
        this.pointingAt = pointingAt;
        this.signature = signature;
        this.encryptedLabel = encryptedLabel;
    }

    public LabelledData(
            SecretIdentity owner2,
            String label,
            HashIdentifier id)
                    throws FriendlyBackupException {
        this(owner2, owner2.encryptConsistently(label.getBytes()), id);
    }

    public boolean verifySignature(PGPPublicKeyRingCollection keyRings)
            throws FriendlyBackupException {
        PublicIdentity pubIdent = new PublicIdentity(keyRings, this.owner);
        Signature origSig = this.signature;

        boolean retval;
        try {
            this.signature = Signature.INTERNAL_SELF_SIGNED;
            retval = origSig.verify(pubIdent, toProto().toByteArray());
        } finally {
            this.signature = origSig;
        }
        
        return retval;
    }

    public String getLabel(SecretIdentity authenticatedOwner)
            throws FriendlyBackupException {
        return new String(authenticatedOwner.decrypt(encryptedLabel));
    }

    public PublicIdentityHandle getOwner() {
        return owner;
    }

    public HashIdentifier getPointingAt() {
        return pointingAt;
    }

    private void sign(SecretIdentity authenticatedUser) throws FriendlyBackupException {
        //can't sign the signature; make sure it has a known value
        this.signature = Signature.INTERNAL_SELF_SIGNED;
        this.signature = authenticatedUser.sign(toProto().toByteArray());
    }
    
    /**
     * LabelledData is special in that the HashID only depends on
     * the owner's public identity handle and the label.  The "pointingAt"
     * will change over time, but we need to be able to find the labelled data
     * in a consistent way.  Also, of course, the signature depends on
     * the value of "pointingAt".
     */
    public HashIdentifier getHashID() {
        return getHashID(getOwner(), encryptedLabel);
    }

    public Basic.LabelledData toProto() {
        Basic.LabelledData.Builder bldrId = Basic.LabelledData.newBuilder();
        
        bldrId.setVersion(1);
        bldrId.setPointingAt(getPointingAt().toProto());
        bldrId.setHashedPortion(getHashedPortionProto(owner, encryptedLabel));
        bldrId.setSignature(this.signature.toProto());
        
        return bldrId.build();
    }

    /**
     * Get the proto for the hashed portion of a piece of LabelledData.
     * @param owner
     * @param encryptedLabel2
     * @return
     */
    public static Basic.LabelledData.HashedPortion getHashedPortionProto(PublicIdentityHandle owner, byte[] encryptedLabel2) {
        Basic.LabelledData.HashedPortion.Builder hpBuilder = Basic.LabelledData.HashedPortion.newBuilder();
        hpBuilder.setOwnerHandle(owner.toProto());
        hpBuilder.setEncryptedLabel(ByteString.copyFrom(encryptedLabel2));
        Basic.LabelledData.HashedPortion hp = hpBuilder.build();
        return hp;
    }

    /**
     * Get the hash id for the LabelledData with the given owner & label.
     * @param owner
     * @param encryptedLabel2
     * @return
     */
    public static HashIdentifier getHashID(
            PublicIdentityHandle owner,
            byte[] encryptedLabel2) {
        return HashIdentifier.hashForBytes(getHashedPortionProto(owner, encryptedLabel2).toByteArray());
    }

    public static LabelledData fromProto(Basic.LabelledData proto) throws FriendlyBackupException {
        versionCheck(1, proto.getVersion(), proto);
        byte[] encryptedLabel = proto.getHashedPortion().getEncryptedLabel().toByteArray();
        PublicIdentityHandle ownerHandle = PublicIdentityHandle.fromProto(proto.getHashedPortion().getOwnerHandle());
        HashIdentifier pointingAt = HashIdentifier.fromProto(proto.getPointingAt());
        Signature signature = Signature.fromProto(proto.getSignature());
        
        return new LabelledData(encryptedLabel, ownerHandle, signature, pointingAt);
    }

    public static HashIdentifier getHashID(
            SecretIdentity authenticatedOwner,
            String label)
                    throws FriendlyBackupException {
        byte[] encryptedLabel2 =
                authenticatedOwner.encryptConsistently(
                        label.getBytes());
        return getHashID(
                authenticatedOwner.getPublicIdentity().getHandle(),
                encryptedLabel2);
    }

}
