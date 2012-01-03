package com.geekcommune.friendlybackup.erasure;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.onionnetworks.util.Buffer;

/**
 * Tool for finding the erasures of a block of data.
 * @author bobbym
 *
 */
public class BytesErasureFinder implements ErasureFinder {

	private int totalErasures;
    private int erasuresNeeded;
    private byte[] data;

    /**
     * Note that this expects that 'data' is already encrypted.
     * @param data
     * @param erasuresNeeded
     * @param totalErasures
     */
    public BytesErasureFinder(byte[] data, int erasuresNeeded, int totalErasures) {
	    this.data = data;
	    this.erasuresNeeded = erasuresNeeded;
	    this.totalErasures = totalErasures;
	}

	public Buffer getErasure(int idx) throws FriendlyBackupException {
	    Buffer[] erasures =
	            ErasureUtil.encode(
	                    data,
	                    erasuresNeeded,
	                    totalErasures);
		return erasures[idx];
	}

}
