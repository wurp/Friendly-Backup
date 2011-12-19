package com.geekcommune.friendlybackup.erasure;

import com.onionnetworks.util.Buffer;

public class BytesErasureFinder implements ErasureFinder {

	private int totalErasures;
    private int erasuresNeeded;
    private byte[] data;

    public BytesErasureFinder(byte[] data, int erasuresNeeded, int totalErasures) {
	    this.data = data;
	    this.erasuresNeeded = erasuresNeeded;
	    this.totalErasures = totalErasures;
	}

	public Buffer getErasure(int idx) {
	    Buffer[] erasures = ErasureUtil.encode(data, erasuresNeeded, totalErasures);
		return erasures[idx];
	}

}
