package com.geekcommune.friendlybackup.datastore;

import java.sql.Timestamp;
import java.util.Date;

import com.geekcommune.identity.PublicIdentityHandle;
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
public class Lease {

    private Date expiration;
    private PublicIdentityHandle owner;
    private boolean soft;
    private Signature signature;

    /**
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
     */
    public Lease(Date expiration, PublicIdentityHandle owner,
            Signature signature, boolean isSoft) {
        this.expiration = expiration;
        this.owner = owner;
        this.signature = signature;
        this.soft = isSoft;
    }

    public Lease(Date date, PublicIdentityHandle owner,
            Signature signature) {
        this(date, owner, signature, false);
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
}
