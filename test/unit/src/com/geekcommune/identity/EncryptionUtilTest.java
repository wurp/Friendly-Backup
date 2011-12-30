package com.geekcommune.identity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.TestCase;

import org.junit.Assert;

import com.geekcommune.util.StringUtil;

public class EncryptionUtilTest extends TestCase {
    public void testEncryption() throws Exception {
        URL url = getClass().getResource("test-input.txt");
        File inFile;
        try {
            inFile = new File(url.toURI());
        } catch(URISyntaxException e) {
            inFile = new File(url.getPath());
        }
        
        String recipient = "bobby";
        char[] passwd = "password".toCharArray();

        InputStream publicRing = getClass().getResourceAsStream("test-pubkeyring.asc");
        InputStream secretRing = getClass().getResourceAsStream("test-seckeyring.gpg");;

        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
        EncryptionUtil.instance().encryptFile(encryptedOut, inFile, publicRing, secretRing, recipient, passwd);
        
        byte[] encryptedBytes = encryptedOut.toByteArray();
        System.out.println("Encrypted: " + StringUtil.hexdump(encryptedBytes));

        publicRing = getClass().getResourceAsStream("test-pubkeyring.asc");
        secretRing = getClass().getResourceAsStream("test-seckeyring.gpg");;

        InputStream encryptedIn = new ByteArrayInputStream(encryptedBytes);
        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();
        EncryptionUtil.instance().decryptKeyBasedFile(decryptedOut, encryptedIn, publicRing, secretRing, passwd);
        
        byte[] out = decryptedOut.toByteArray();
        byte[] expected = "Some stuff\nMore stuff\nHi Bobby!\n\n".getBytes();
        Assert.assertArrayEquals(expected, out);
    }
}
