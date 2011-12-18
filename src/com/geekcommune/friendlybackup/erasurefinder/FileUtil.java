package com.geekcommune.friendlybackup.erasurefinder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtil {

	public byte[] getFileContents(File f) throws IOException {
	    //TODO handle files > 2 gig
		byte[] retval = new byte[(int)f.length()];
		
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
		try {
	        bis.read(retval);
		} finally {
		    bis.close();
		}
		
		return retval;
	}

	public static FileUtil instance() {
		return new FileUtil();
	}

}
