package com.geekcommune.friendlybackup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Assert;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendDataMessage;
import com.geekcommune.friendlybackup.communication.message.VerifyMaybeSendErasureMessage;
import com.geekcommune.friendlybackup.config.SwingPassphraseDialog;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.identity.PublicIdentityHandle;
import com.geekcommune.util.StringUtil;

/**
 * A place to stick random little tests.
 * @author bobbym
 */
public class TestGrabBag {
    private static HashIdentifier hashId = makeRandomHashIdentifier();

    @Test
    @Ignore
    public void todo_needs_SecretIdentity_testVerifyMaybeSendDataMessage() throws Exception {
        VerifyMaybeSendErasureMessage vmsem =
                new VerifyMaybeSendErasureMessage(
                        new RemoteNodeHandle("test", "test@foo.com", "localhost:123", new PublicIdentityHandle(0, 0)),
                        123,
                        hashId,
                        null,
                        0,
                        null) {
            
            @Override
            public byte[] getData() {
                return new byte[0];
            }
        };

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        {
            DataOutputStream dos = new DataOutputStream(baos);
            vmsem.write(dos);
            dos.write(123);
            dos.flush();
        }

        byte[] buff = baos.toByteArray();
        System.out.println(vmsem.getDataHashID());
        System.out.println(StringUtil.hexdump(buff));

        ByteArrayInputStream bais = new ByteArrayInputStream(buff);
        DataInputStream dis = new DataInputStream(bais);
        dis.readInt(); //msgType
        VerifyMaybeSendDataMessage actual = (VerifyMaybeSendDataMessage) VerifyMaybeSendErasureMessage.FACTORY.makeMessage();
        actual.read(dis);
        dis.read();

        //Assert.assertEquals(expected, actual);
    }

    @Test
    public void testProtoStream() throws Exception {
        HashIdentifier expected = hashId;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(123);
        dos.writeInt(123);
        dos.writeInt(123);
        //this test will fail if you use the non-Delimited version of writeTo and parseFrom
        expected.toProto().writeDelimitedTo(baos);
        dos.writeInt(123);
        baos.flush();

        byte[] buff = baos.toByteArray();
        System.out.println(StringUtil.hexdump(buff));
        System.out.println(expected);

        ByteArrayInputStream bais = new ByteArrayInputStream(buff);
        DataInputStream dis = new DataInputStream(bais);
        dis.readInt();
        dis.readInt();
        dis.readInt();
        HashIdentifier actual = HashIdentifier.fromProto(Basic.HashIdentifier.parseDelimitedFrom(bais));
        dis.readInt();
        
        Assert.assertEquals(expected, actual);
    }

    public static HashIdentifier makeRandomHashIdentifier() {
        byte[] digest = new byte[HashIdentifier.SIZEOF];
        
        for(int i = 0; i < digest.length; ++i) {
            digest[i] = (byte)((Math.random() - .5) * 256);
        }
        
        HashIdentifier expected = new HashIdentifier(digest);
        return expected;
    }
    
    @Test
    @Ignore
    public void testPwdDialog() throws Exception {
        SwingPassphraseDialog dlg = new SwingPassphraseDialog();
        Assert.assertNotNull(dlg.getPassphrase());
        System.out.println(dlg.getPassphrase());
    }
}
