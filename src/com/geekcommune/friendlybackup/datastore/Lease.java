package com.geekcommune.friendlybackup.datastore;

import java.util.Date;

import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.identity.PublicIdentity;
import com.geekcommune.identity.PublicIdentityHandle;
import com.geekcommune.identity.SecretIdentity;
import com.geekcommune.identity.Signature;

/**
 * Tells how long we have been asked to do something - so far the only
 * use case is how long we should store a piece of data.
 * 
 * See also com.geekcommune.friendlybackup.format.high.Lease for
 * the complete, signed, streamable lease.
 * 
 * @author bobbym
 *
 */
public class Lease extends BaseData<Basic.Lease>{

    private Date expiration;
    private PublicIdentityHandle owner;
    private boolean soft;
    private Signature signature;
    private HashIdentifier leasedData;

    /**
     * Use when you are rebuilding an already signed lease.
     * 
     * Embodies a lease for the data identified by key
     * requesting (by owner) that the data be stored
     * until expiration.  isSoft == true means the lease-holder
     * (owner) would like the data to be stored, but it is
     * not critical.
     * 
     * Don't forget to validate the lease!
     * 
     * @param expiration
     * @param owner
     * @param signature
     * @param isSoft
     */
    public Lease(Date expiration, PublicIdentityHandle owner,
            Signature signature, boolean isSoft, HashIdentifier leasedData) {
        this.expiration = expiration;
        this.owner = owner;
        this.signature = signature;
        this.soft = isSoft;
        this.leasedData = leasedData;
    }

    /**
     * Use when you want to sign the lease.
     * 
     * Embodies a lease for the data identified by key
     * requesting (by owner) that the data be stored
     * until expiration.  isSoft = true means the lease-holder
     * (owner) would like the data to be stored, but it is
     * not critical.
     * 
     * @param expiration
     * @param owner
     * @param signature
     * @param isSoft
     * @throws FriendlyBackupException 
     */
    public Lease(
            Date expiration,
            SecretIdentity authenticatedUser,
            boolean isSoft,
            HashIdentifier leasedData)
                    throws FriendlyBackupException {
        this.expiration = expiration;
        this.owner = authenticatedUser.getPublicIdentity().getHandle();
        this.soft = isSoft;
        this.leasedData = leasedData;
        sign(authenticatedUser);
    }

    public Lease(Date date, PublicIdentityHandle owner,
            Signature signature, HashIdentifier leasedData) {
        this(date, owner, signature, false, leasedData);
    }

    private void sign(SecretIdentity authenticatedUser) throws FriendlyBackupException {
        //can't sign the signature; make sure it has a known value
        this.signature = Signature.INTERNAL_SELF_SIGNED;
        this.signature = authenticatedUser.sign(toProto().toByteArray());
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

    public Date getExpiry() {
        return expiration;
    }

    public PublicIdentityHandle getOwner() {
        return owner;
    }

    public boolean isSoft() {
        return soft;
    }

    public HashIdentifier getLeasedData() {
        return this.leasedData;
    }

    public Signature getSignature() {
        return this.signature;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder("Lease(");
        sb.append(this.expiration).append(",");
        sb.append(this.owner).append(",");
        sb.append(this.soft).append(",");
        sb.append(this.leasedData).append(",");
        sb.append(this.signature);
        
        return sb.toString();
    }

    public com.geekcommune.friendlybackup.proto.Basic.Lease toProto() {
        Basic.Lease.Builder bldrId = Basic.Lease.newBuilder();
        
        bldrId.setVersion(1);
        bldrId.setExpiration(getExpiry().getTime());
        bldrId.setOwnerHandle(getOwner().toProto());
        bldrId.setSignature(getSignature().toProto());
        bldrId.setSoft(isSoft());
        bldrId.setLeasedData(getLeasedData().toProto());
        
        return bldrId.build();
    }

    public static Lease fromProto(Basic.Lease proto) throws FriendlyBackupException {
        versionCheck(1, proto.getVersion(), proto);
        
        Date expiry = new Date(proto.getExpiration());
        PublicIdentityHandle ownerHandle = PublicIdentityHandle.fromProto(proto.getOwnerHandle());
        Signature sig = Signature.fromProto(proto.getSignature());
        boolean soft = proto.getSoft();
        HashIdentifier leasedData = HashIdentifier.fromProto(proto.getLeasedData());
        
        return new Lease(expiry, ownerHandle, sig, soft, leasedData);
    }
}
