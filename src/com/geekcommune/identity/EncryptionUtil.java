package com.geekcommune.identity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;

public class EncryptionUtil {
    private static final Logger log = Logger.getLogger(EncryptionUtil.class);

    public void init() {
        Security.addProvider(new BouncyCastleProvider());
        PGPUtil.setDefaultProvider("BC");

    }
    
    public void readKeyRing(String baseDir) throws FileNotFoundException, IOException,
            PGPException {
        PGPPublicKeyRingCollection pubRings = null;
        PGPPublicKeyRing pgpPub = null;
        InputStream in = null;

        // directory that contains all the .asc files
        File dir = new File(baseDir +
                "/KeyRings/Public");

        // list all the files
        String[] children = dir.list();
        if (children == null) {
            // Either dir does not exist or is not a directory
        } else {
            for (int i = 0; i < children.length; i++) {
                if (i == 0) {
                    // read the first .asc file and create the
                    // PGPPublicKeyRingCollection to hold all the other key
                    // rings
                    log.info("File Name (.asc) (0) = " + children[0]);
                    in = PGPUtil.getDecoderStream(new FileInputStream(new File(dir, children[0])));
                    pubRings = new PGPPublicKeyRingCollection(in);
                } else {
                    String filename = children[i];
                    log.info("File Name (.asc) " + "(" + i + ")"
                            + " = " + filename);
                    in = PGPUtil.getDecoderStream(new FileInputStream(new File(dir, filename)));
                    PGPPublicKeyRingCollection otherKeyRings = new PGPPublicKeyRingCollection(
                            in);

                    @SuppressWarnings("unchecked")
                    Iterator<PGPPublicKeyRing> rIt = otherKeyRings
                            .getKeyRings();
                    while (rIt.hasNext()) {
                        pgpPub = rIt.next();
                    }
                    
                    // copy the key ring to PGPPublicKeyCollection pubRings
                    pubRings = PGPPublicKeyRingCollection.addPublicKeyRing(
                            pubRings, pgpPub);
                    // size should equal the number of the .asc files
                    log.debug("Collection size = "
                            + pubRings.size());
                }
            }// end of for
        }// end of else
    }
}
