package com.geekcommune.communication.handler;

import com.geekcommune.communication.Event;

public interface Handler {
    /**
     * Handle an event this handler checks.  The semantics of whether this
     * may be called when the event is not ready, and what custom data
     * is in the event, depends on the specific handler.
     *  
     * @param obj
     */
    public void handle(Event obj);
}
