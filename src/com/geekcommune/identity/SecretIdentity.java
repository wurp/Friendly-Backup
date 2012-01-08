package com.geekcommune.identity;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;

import com.geekcommune.friendlybackup.FriendlyBackupException;
import com.geekcommune.friendlybackup.datastore.Lease;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;


public class SecretIdentity {
    PGPSecretKey signingKey;
    PGPSecretKey encryptingKey;
    private char[] passphrase;
    private PublicIdentity pi;
    
    protected SecretIdentity() {
    }
    
	public SecretIdentity(
	        PGPPublicKeyRing pubKeyRing,
	        PGPSecretKeyRingCollection secKeyRings,
            char[] passphrase) throws FriendlyBackupException {
        String exceptionMessage = "Could not find all necessary keys for secret identity";
        
        try {
            this.signingKey =
                    EncryptionUtil.instance().findFirstSigningKey(
                            pubKeyRing,
                            secKeyRings);
            PGPPublicKey pubEncryptingKey =
                    EncryptionUtil.instance().findFirstEncryptingKey(
                            pubKeyRing);
            this.encryptingKey =
                    EncryptionUtil.instance().findSecretKey(
                            secKeyRings,
                            pubEncryptingKey.getKeyID(),
                            false);
            
            this.passphrase = passphrase;
            
            this.pi =
                    new PublicIdentity(
                            pubKeyRing,
                            new PublicIdentityHandle(
                                    this.signingKey.getPublicKey(),
                                    this.encryptingKey.getPublicKey()));
        } catch (PGPException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (NoSuchProviderException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (IOException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        }
    }

    public PublicIdentity getPublicIdentity() {
		return pi;
	}

	public Signature sign(byte[] data) throws FriendlyBackupException {
		final String exceptionMessage = "Could not create pgp signature";
		
        try {
            PGPSignature pgpSignature =
                    EncryptionUtil.instance().makeSignature(
                            data,
                            signingKey.getPublicKey(),
                            signingKey.extractPrivateKey(
                                    passphrase,
                                    "BC"));
            return new Signature(pgpSignature);
        } catch (NoSuchAlgorithmException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (NoSuchProviderException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (SignatureException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (PGPException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        }
	}

	/**
	 * Encrypt the data with my PUBLIC key, but in such a way that it
	 * will:
	 *   a) always produce the same output for the same input (for me)
	 *   b) NOT produce the same output for the same input for anyone else (so they can't go fishing to see what my data is, or build rainbow tables of known files, etc)
	 *   c) not use the same keystream for different inputs
	 *   
	 * You can decrypt this data as normal using the private key; i.e. there is no need for a decryptConsistently(); just use decrypt().
	 * @param data
	 * @return
	 * @throws FriendlyBackupException
	 */
	public byte[] encryptConsistently(byte[] data) throws FriendlyBackupException {
	    final String exceptionMessage = "Could not encrypt";
	    
	    try {
	        //generate sha256 hash of input data as part of the seed
            MessageDigest digest = MessageDigest.getInstance("SHA256");
            byte[] dataHash = digest.digest(data);

            //use data that only I have access to as the other part, so other people can't fish for my data
            byte[] pvtKeyHash;
            
            {
                byte[] pvtKey = this.encryptingKey.extractPrivateKey(passphrase, "BC").getKey().getEncoded();

                digest = MessageDigest.getInstance("SHA256");
                pvtKeyHash = digest.digest(pvtKey);
            }

            
            byte[] seed = new byte[pvtKeyHash.length + dataHash.length];
            System.arraycopy(pvtKeyHash, 0, seed, 0, pvtKeyHash.length);
            System.arraycopy(dataHash, 0, seed, pvtKeyHash.length, dataHash.length);
            
            return EncryptionUtil.instance().encryptData(data, this.encryptingKey.getPublicKey(), passphrase, seed);
        } catch (NoSuchAlgorithmException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (NoSuchProviderException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (PGPException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (IOException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        }
	}
	
	public byte[] decrypt(byte[] encryptedData) throws FriendlyBackupException {
	    final String exceptionMessage = "Could not decrypt data";
	    
        try {
            return EncryptionUtil.instance().decryptKeyBasedData(encryptedData, this.encryptingKey, passphrase);
        } catch (PGPException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        } catch (IOException e) {
            throw new FriendlyBackupException(exceptionMessage, e);
        }
	}
	
	/**
	 * Create and sign a lease for a piece of data
	 * 
	 * @param expiryDate
	 * @return
	 * @throws FriendlyBackupException 
	 */
    public Lease makeLease(HashIdentifier id, Date expiryDate) throws FriendlyBackupException {
        return new Lease(
                expiryDate,
                this,
                false,
                id);
    }

    public void validatePassphrase(char[] passphrase2) throws FriendlyBackupException {
        if( !Arrays.equals(passphrase, passphrase2) ) {
            throw new FriendlyBackupException("Passphrase mismatch");
        }
    }

}
