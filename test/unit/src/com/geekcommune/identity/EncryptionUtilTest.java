package com.geekcommune.identity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import junit.framework.TestCase;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.junit.Assert;

import com.geekcommune.util.Pair;
import com.geekcommune.util.StringUtil;

public class EncryptionUtilTest extends TestCase {
    private PGPPublicKeyRingCollection pubRing;
    private PGPSecretKeyRingCollection secRing;
    private String recipient;
    private char[] passwd;

    public void setUp() throws Exception {
        InputStream publicRing = getClass().getResourceAsStream("test-pubkeyring.asc");
        pubRing = EncryptionUtil.instance().readPublicKeyRingCollection(publicRing);
        publicRing.close();

        InputStream secretRing = getClass().getResourceAsStream("test-seckeyring.gpg");
        secRing = EncryptionUtil.instance().readSecretKeyRingCollection(secretRing);
        secretRing.close();
        
        recipient = "bobby";
        passwd = "password".toCharArray();
    }
    
    public void testFileEncryption() throws Exception {
        URL url = getClass().getResource("test-input.txt");
        File inFile;
        try {
            inFile = new File(url.toURI());
        } catch(URISyntaxException e) {
            inFile = new File(url.getPath());
        }

        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
        EncryptionUtil.instance().encryptFile(encryptedOut, inFile, pubRing, secRing, recipient, passwd);

        byte[] encryptedBytes = encryptedOut.toByteArray();
        System.out.println("Encrypted: " + StringUtil.hexdump(encryptedBytes));

        InputStream encryptedIn = new ByteArrayInputStream(encryptedBytes);
        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();
        EncryptionUtil.instance().decryptKeyBasedFile(decryptedOut, encryptedIn, pubRing, secRing, passwd);
        
        byte[] out = decryptedOut.toByteArray();
        byte[] expected = "Some stuff\nMore stuff\nHi Bobby!\n\n".getBytes();
        Assert.assertArrayEquals(expected, out);
        
        encryptedOut.close();
        encryptedIn.close();
        decryptedOut.close();
    }

    public void testBufferEncryption() throws Exception {
        byte[] input = "Some stuff\nMore stuff\nHi Bobby!\n\n".getBytes();

        validateDataEncryption(input, secRing, pubRing, recipient, passwd);
    }

    public void testMediumBufferEncryption() throws Exception {
        byte[] input = new byte[16*1024];

        for(int i = 0; i < input.length; ++i) {
            input[i] = (byte)((Math.random() - 0.5) * 256);
        }
        
        validateDataEncryption(input, secRing, pubRing, recipient, passwd);
    }

    /**
     * Takes a long time to run; omitting from testing for now (passes)
     * @throws Exception
     */
    public void _testBigBufferEncryption() throws Exception {
        byte[] input = new byte[64*1024*1024];

        for(int i = 0; i < input.length; ++i) {
            input[i] = (byte)((Math.random() - 0.5) * 256);
        }
        
        validateDataEncryption(input, secRing, pubRing, recipient, passwd);
    }

    private void validateDataEncryption(
            byte[] input,
            PGPSecretKeyRingCollection secretRing,
            PGPPublicKeyRingCollection publicRing,
            String recip,
            char[] pass) throws PGPException,
            IOException {
        long before = System.currentTimeMillis();
        byte[] encrypted =
                EncryptionUtil.instance().encryptData(input, publicRing, secretRing, recip, pass);
        long elapsed = System.currentTimeMillis() - before;
        
        if( encrypted.length > 32 ) {
            byte[] header = new byte[16];
            System.arraycopy(encrypted, 0, header, 0, header.length);
            System.out.println("Encrypted data header: " + StringUtil.hexdump(header));
            
            byte[] footer = new byte[16];
            System.arraycopy(encrypted, encrypted.length - footer.length, footer, 0, footer.length);
            System.out.println("Encrypted data header: " + StringUtil.hexdump(footer));
        } else {
            System.out.println("Encrypted data: " + StringUtil.hexdump(encrypted));
        }
        System.out.println("Raw data length: " + input.length);
        System.out.println("Encrypted data length: " + encrypted.length);
        System.out.println("Encryption took (in millis): " + elapsed);

        byte[] decrypted =
                EncryptionUtil.instance().decryptKeyBasedData(encrypted, pubRing, secRing, passwd);
        
        Assert.assertArrayEquals(input, decrypted);
    }

    public void testCreateKeyringIfNone() throws Exception {
        fail("unimplemented");

        File keyRingDir = new File("test/unit/encrypt-working");
        keyRingDir.mkdirs();
        
        File secretKeyRingFile = new File(keyRingDir, "secring.gpg");
        File publicKeyRingFile = new File(keyRingDir, "pubring.asc");
        
        secretKeyRingFile.delete();
        publicKeyRingFile.delete();
        
        EncryptionUtil.instance().getOrCreateKeyring(publicKeyRingFile, secretKeyRingFile, new KeyDataSource() {

            public char[] getPassphrase() {
                // TODO Auto-generated method stub
                return null;
            }

            public String getIdentity() {
                // TODO Auto-generated method stub
                return null;
            }
            
        });
    }

    public void testLoadKeyring() throws Exception {
        File keyRingDir = new File("test/unit/encrypt-working");
        keyRingDir.mkdirs();
        
        File secretKeyRingFile = new File(keyRingDir, "secring.gpg");
        File publicKeyRingFile = new File(keyRingDir, "pubring.asc");
        
        Pair<PGPPublicKeyRingCollection, PGPSecretKeyRingCollection> keys =
                EncryptionUtil.instance().getOrCreateKeyring(
                        publicKeyRingFile,
                        secretKeyRingFile,
                        new KeyDataSource() {

                            public char[] getPassphrase() {
                                return passwd;
                            }

                            public String getIdentity() {
                                return recipient;
                            }
            
                        });
        
        byte[] input = { 0, 0, 1, 2, 3, 4, 5, 6, 7, -7, -6, -5, -4, -3, -2, -1, 0, 0 };
        validateDataEncryption(input, keys.getSecond(), keys.getFirst(), recipient, passwd);
    }
    
    public void testSignaturePositive() throws Exception {
        PGPPrivateKey privkey = null;
        PGPPublicKey pubkey = null;
        byte[] input = null;
        EncryptionUtil.instance().
            makeSignature(input, pubkey, privkey);
    }
}
