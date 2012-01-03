package com.geekcommune.identity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureList;

public class PGPSignatureCheat {

    /**
     * Reverse of PGPSignature.encode()
     * 
     * Ugly code to work around missing bits in BC API.
     * 
     * @param data
     * @return
     * @throws IOException
     */
    public static PGPSignature newPGPSignature(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        
        PGPSignatureList sigList =
                (PGPSignatureList) new PGPObjectFactory(bais).nextObject();
        
        return sigList.get(0);
    }

}
