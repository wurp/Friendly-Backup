package com.geekcommune.friendlybackup.erasure;

/**
 * Pingable objects can be 'ping'ed, in response to which they may or may not do something.
 * For example, a timed event could check if it's time for it to run again, and if so, it executes.
 * @author bobbym
 *
 */
public interface Pingable {

	public void ping();
}
