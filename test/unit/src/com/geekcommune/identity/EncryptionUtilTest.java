package com.geekcommune.identity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.junit.Assert;

import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.format.low.LabelledData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.util.FileUtil;
import com.geekcommune.util.Pair;
import com.geekcommune.util.StringUtil;

public class EncryptionUtilTest extends TestCase {
    private PGPPublicKeyRingCollection pubRing;
    private PGPSecretKeyRingCollection secRing;
    private String recipient;
    private char[] passwd;

    public void setUp() throws Exception {
        //initialize encryption (e.g. load provider)
        EncryptionUtil.instance();
        InputStream publicRing = getClass().getResourceAsStream("test-pubkeyring.asc");
        pubRing = EncryptionUtil.instance().readPublicKeyRingCollection(publicRing);
        publicRing.close();

        InputStream secretRing = getClass().getResourceAsStream("test-seckeyring.gpg");
        secRing = EncryptionUtil.instance().readSecretKeyRingCollection(secretRing);
        secretRing.close();
        
        recipient = "bobby";
        passwd = "password".toCharArray();
    }
    
    public void testEncryptConsistently() throws Exception {
        for(int i = 0; i < 50; ++i) {
            helpEncryptConsistently(100);
        }
    }
    
    public void helpEncryptConsistently(int len) throws Exception {
        //find the signing key
        PGPPublicKeyRing pubKeyRing = (PGPPublicKeyRing) pubRing.getKeyRings().next();

        //make the GeekCommune version of the signing/identity classes
        SecretIdentity secretIdent = new SecretIdentity(pubKeyRing, secRing, passwd);
        
        byte[] original = new byte[len];
        Random rand = new Random(1);
        rand.nextBytes(original);
        
        byte[] encrypted1 = secretIdent.encryptConsistently(original);
        byte[] decrypted = secretIdent.decrypt(encrypted1);
        
        Assert.assertArrayEquals("decrypted content not same as original", original, decrypted);
        
        byte[] encrypted2 = secretIdent.encryptConsistently(original);
        byte[] decrypted2 = secretIdent.decrypt(encrypted2);
        Assert.assertArrayEquals("decrypted content2 not same as original", original, decrypted2);
        Assert.assertArrayEquals("encryption was not consistent", encrypted1, encrypted2);
    }
    
    public void testEncryptConsistently2() throws Exception {
        //find the signing key
        PGPPublicKeyRing pubKeyRing = (PGPPublicKeyRing) pubRing.getKeyRings().next();

        //make the GeekCommune version of the signing/identity classes
        SecretIdentity secretIdent = new SecretIdentity(pubKeyRing, secRing, passwd);
        
        byte[] original = FileUtil.instance().getFileContents(new File("test/integ/happy1/config/dir-to-backup/some-bin/java.exe"));
        Random rand = new Random(1);
        rand.nextBytes(original);
        
        byte[] encrypted1 = secretIdent.encryptConsistently(original);
        byte[] decrypted = secretIdent.decrypt(encrypted1);
        
        Assert.assertArrayEquals("decrypted content not same as original", original, decrypted);
        
        byte[] encrypted2 = secretIdent.encryptConsistently(decrypted);
        Assert.assertArrayEquals("encryption was not consistent", encrypted1, encrypted2);
    }
    
    public void testSecureRandomConsistency() throws Exception {
        byte[] seed = new byte[] { 1, 1, 1, 1 };
        SecureRandom secRand = new SecureRandom(seed);
        
        byte[] randBuff1 = new byte[1000000];
        secRand.nextBytes(randBuff1);
        
        SecureRandom secRand2 = new SecureRandom(seed);
        
        byte[] randBuff2 = new byte[1000000];
        secRand2.nextBytes(randBuff2);
        
        Assert.assertArrayEquals(randBuff1, randBuff2);
    }
    public void testSignatureSerialization() throws Exception {
        //find the signing key
        PGPPublicKeyRing pubKeyRing = (PGPPublicKeyRing) pubRing.getKeyRings().next();

        //make the GeekCommune version of the signing/identity classes
        SecretIdentity secretIdent = new SecretIdentity(pubKeyRing, secRing, passwd);
        PublicIdentity pubIdent = secretIdent.getPublicIdentity();

        //sign some data
        byte[] bytesToSign = new byte[] { -1, 0, 1, 2, 3 };
        Signature sig = secretIdent.sign(bytesToSign);

        //stream the signature to & fro
        Basic.Signature proto = sig.toProto();
        Signature recoveredSig = Signature.fromProto(proto);
        
        //verify the parsed sig works (twice)
        Assert.assertTrue(recoveredSig.verify(pubIdent, bytesToSign));
        Assert.assertTrue(recoveredSig.verify(pubIdent, bytesToSign));
        
        //verify that corrupted data does not work
        bytesToSign[0] = 15;
        Assert.assertFalse(recoveredSig.verify(pubIdent, bytesToSign));
    }
    
    public void testFileErasureFinder() throws Exception {
        
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
        EncryptionUtil.instance().encryptFile(encryptedOut, inFile, pubRing, secRing, recipient, passwd, null);

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
                EncryptionUtil.instance().encryptData(input, null, publicRing, secretRing, recip, pass, null);
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
        
        byte[] input = makeInput1();
        validateDataEncryption(input, keys.getSecond(), keys.getFirst(), recipient, passwd);
    }

    private byte[] makeInput1() {
        byte[] input = { 0, 0, 1, 2, 3, 4, 5, 6, 7, -7, -6, -5, -4, -3, -2, -1, 0, 0 };
        return input;
    }
    
    public void testFBEncryption() throws Exception {
        //find the signing key
        PGPPublicKeyRing pubKeyRing = (PGPPublicKeyRing) pubRing.getKeyRings().next();

        //make the GeekCommune version of the signing/identity classes
        SecretIdentity secretIdent = new SecretIdentity(pubKeyRing, secRing, passwd);
        
        byte[] input = makeInput1();
        byte[] encrypted = secretIdent.encryptConsistently(input);
        
        Assert.assertFalse(Arrays.equals(input, encrypted));
        
        byte[] decrypted = secretIdent.decrypt(encrypted);
        
        Assert.assertTrue(Arrays.equals(input, decrypted));
        
        //make sure it really is consistent...
        byte[] encrypted2 = secretIdent.encryptConsistently(input);
        Assert.assertArrayEquals(encrypted, encrypted2);
    }
    
    public void testLabelledDataSignVerify() throws Exception {
        //find the signing key
        PGPPublicKeyRing pubKeyRing = (PGPPublicKeyRing) pubRing.getKeyRings().next();

        //make the GeekCommune version of the signing/identity classes
        SecretIdentity secretIdent = new SecretIdentity(pubKeyRing, secRing, passwd);

        Random rand = new Random();
        byte[] hashData = new byte[HashIdentifier.SIZEOF];
        rand.nextBytes(hashData);
        
        LabelledData ld = new LabelledData(secretIdent, "bamfoozle", new HashIdentifier(hashData));
        
        byte[] ldBytes = ld.toProto().toByteArray();
        Basic.LabelledData proto = Basic.LabelledData.parseFrom(ldBytes);
        LabelledData ldFromBytes = LabelledData.fromProto(proto);
        
        ldFromBytes.verifySignature(pubRing);
    }
}
