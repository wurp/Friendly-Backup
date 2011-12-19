package com.geekcommune.friendlybackup.format.low;

import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.identity.PrivateIdentity;
import com.geekcommune.identity.PublicIdentityHandle;
import com.geekcommune.identity.Signature;

public class LabelledData extends BaseData<Basic.LabelledData> {

	private String label;
    private PublicIdentityHandle owner;
    private Signature signature;
    private HashIdentifier pointingAt;

    public LabelledData(PrivateIdentity owner, String label, HashIdentifier pointingAt) {
        this.label = label;
        this.owner = owner.getPublicIdentity().getHandle();
        this.sign(owner);
        this.pointingAt = pointingAt;
    }

    protected LabelledData(String label, PublicIdentityHandle ownerHandle,
            Signature signature, HashIdentifier pointingAt) {
        this.owner = ownerHandle;
        this.pointingAt = pointingAt;
        this.signature = signature;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public PublicIdentityHandle getOwner() {
        return owner;
    }

    public HashIdentifier getPointingAt() {
        return pointingAt;
    }

	private void sign(PrivateIdentity authenticatedUser) {
	    this.signature = Signature.Dummy;
	    this.pointingAt = HashIdentifier.Dummy;
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
		return getHashID(getOwner(), label);
	}

    public Basic.LabelledData toProto() {
        Basic.LabelledData.Builder bldrId = Basic.LabelledData.newBuilder();
        
        bldrId.setVersion(1);
        bldrId.setPointingAt(getPointingAt().toProto());
        bldrId.setHashedPortion(getHashedPortionProto(owner, label));
        bldrId.setSignature(this.signature.toProto());
        
        return bldrId.build();
    }

    /**
     * Get the proto for the hashed portion of a piece of LabelledData.
     * @param owner
     * @param label
     * @return
     */
    public static Basic.LabelledData.HashedPortion getHashedPortionProto(PublicIdentityHandle owner, String label) {
        Basic.LabelledData.HashedPortion.Builder hpBuilder = Basic.LabelledData.HashedPortion.newBuilder();
        hpBuilder.setOwnerHandle(owner.toProto());
        hpBuilder.setLabel(label);
        Basic.LabelledData.HashedPortion hp = hpBuilder.build();
        return hp;
    }

    /**
     * Get the hash id for the LabelledData with the given owner & label.
     * @param owner
     * @param label
     * @return
     */
    public static HashIdentifier getHashID(PublicIdentityHandle owner,
            String label) {
        return HashIdentifier.hashForBytes(getHashedPortionProto(owner, label).toByteArray());
    }

    public static LabelledData fromProto(Basic.LabelledData proto) {
        versionCheck(1, proto.getVersion(), proto);
        String label = proto.getHashedPortion().getLabel();
        PublicIdentityHandle ownerHandle = PublicIdentityHandle.fromProto(proto.getHashedPortion().getOwnerHandle());
        HashIdentifier pointingAt = HashIdentifier.fromProto(proto.getPointingAt());
        Signature signature = Signature.fromProto(proto.getSignature());
        
        return new LabelledData(label, ownerHandle, signature, pointingAt);
    }

}
