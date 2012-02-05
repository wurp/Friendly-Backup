package com.geekcommune.friendlybackup.erasure;

import java.io.File;
import java.io.IOException;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.identity.SecretIdentity;
import com.geekcommune.util.FileUtil;
import com.onionnetworks.util.Buffer;

public class FileErasureFinder implements ErasureFinder {

	private int totalErasures;
    private int erasuresNeeded;
    private File file;
    private SecretIdentity owner;

    public FileErasureFinder(File f, SecretIdentity owner, int erasuresNeeded, int totalErasures) {
		this.file = f;
		this.owner = owner;
		this.erasuresNeeded = erasuresNeeded;
		this.totalErasures = totalErasures;
	}

	public Buffer getErasure(int idx) throws FriendlyBackupException {
        try {
            Buffer[] erasures = ErasureUtil.encode(
                    owner.encryptConsistently(FileUtil.instance().getFileContents(file)),
                    erasuresNeeded,
                    totalErasures);
            
            return erasures[idx];
        } catch (IOException e) {
            throw new FriendlyBackupException("Failed to find erasure " + idx, e);
        }
	}
}
