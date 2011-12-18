package com.geekcommune.friendlybackup.erasurefinder;

import com.onionnetworks.util.Buffer;

public interface ErasureFinder {
	public Buffer getErasure(int idx);
}
