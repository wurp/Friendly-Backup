package com.geekcommune.friendlybackup.integ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bouncycastle.util.Arrays;

import junit.framework.TestCase;

import com.geekcommune.util.FileUtil;

public class IntegrationTestCase extends TestCase {

    protected boolean compareDirectories(
            File dir1,
            File dir2) throws IOException {
        List<File> allFiles1 = getAllFilesInOrder(dir1);
        List<File> allFiles2 = getAllFilesInOrder(dir2);

        String dir1Canonical = dir1.getCanonicalPath();
        String dir2Canonical = dir2.getCanonicalPath();
        
        if( allFiles1.size() == allFiles2.size() ) {
            for(int i = 0; i < allFiles1.size(); ++i) {
                File file1 = allFiles1.get(i);
                File file2 = allFiles2.get(i);
                
                if( stripRoot(file1, dir1Canonical).equals(stripRoot(file2, dir2Canonical)) ) {
                    if( file1.isDirectory() ) {
                        return file2.isDirectory();
                    } else {
                        byte[] contents1 =
                                FileUtil.instance().getFileContents(file1);
                        byte[] contents2 =
                                FileUtil.instance().getFileContents(file2);
                        
                        if( !Arrays.areEqual(contents1, contents2)) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            }

            //we made it through all the files & they were identical
            return true;
        } else {
            return false;
        }
    }

    private String stripRoot(File file, String root) throws IOException {
        return file.getCanonicalPath().substring(root.length());
    }

    private List<File> getAllFilesInOrder(File dir) {
        List<File> allFiles = new ArrayList<File>();
        FileUtil.instance().listTree(allFiles, dir);
        Collections.sort(allFiles);
        
        return allFiles;
    }

    protected void cleanDirectory(File dir) {
        List<File> allFiles = new ArrayList<File>();
        FileUtil.instance().listTree(allFiles, dir);
        
        for(File file : allFiles) {
            file.delete();
        }
    }
}
