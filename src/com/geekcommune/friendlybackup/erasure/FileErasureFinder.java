package com.geekcommune.friendlybackup.erasure;

import java.io.File;
import java.io.IOException;

import com.geekcommune.util.FileUtil;
import com.onionnetworks.util.Buffer;

public class FileErasureFinder implements ErasureFinder {

	private int totalErasures;
    private int erasuresNeeded;
    private File file;

    public FileErasureFinder(File f, int erasuresNeeded, int totalErasures) {
		this.file = f;
		this.erasuresNeeded = erasuresNeeded;
		this.totalErasures = totalErasures;
	}

	public Buffer getErasure(int idx) {
        Buffer[] erasures;
        try {
            erasures = ErasureUtil.encode(
                    FileUtil.instance().getFileContents(file),
                    erasuresNeeded,
                    totalErasures);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        
        return erasures[idx];
	}

}
