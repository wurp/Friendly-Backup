package com.geekcommune.friendlybackup.datastore;

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

    public Lease(Date expiration, PublicIdentityHandle owner,
            Signature signature) {
        // TODO Auto-generated constructor stub
    }
}
