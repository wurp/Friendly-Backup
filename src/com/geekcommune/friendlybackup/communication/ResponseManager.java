package com.geekcommune.friendlybackup.communication;

import com.geekcommune.util.Continuation;

/**
 * Ensures that a request only gets handled once.  In some cases we send messages to many
 * servers, but only want to process the data one time, as soon as we have the complete set
 * of data.
 * @author bobbym
 *
 */
public class ResponseManager {

    private boolean done;
    private int responsesNeededForQuorum;
    private int responses;

    public ResponseManager(int responsesNeededForQuorum) {
        this.responsesNeededForQuorum = responsesNeededForQuorum;
    }

    public ResponseManager() {
        this(1);
    }

    public void doOnce(Continuation continuation) {
        boolean doNow = false;
        
        synchronized(this) {
            ++responses;
            
            if( responses >= responsesNeededForQuorum ) {
                if( !done ) {
                    doNow = true;
                }

                done = true;
            }
        }
        
        if( doNow ) {
            continuation.run();
        }
    }
}
