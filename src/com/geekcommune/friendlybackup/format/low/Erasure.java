package com.geekcommune.friendlybackup.format.low;

import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.google.protobuf.ByteString;
import com.onionnetworks.util.Buffer;

public class Erasure extends BaseData<Basic.Erasure> {

    private com.geekcommune.friendlybackup.erasure.Erasure plainErasure;

    public Erasure(Buffer buffer, int i) {
        plainErasure = new com.geekcommune.friendlybackup.erasure.Erasure(buffer.getBytes(), i);
    }

    public void setIndex(int index) {
        plainErasure.setIndex(index);
    }

    public int getIndex() {
        return plainErasure.getIndex();
    }

    public byte[] getErasureContents() {
        return plainErasure.getErasureContents();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": idx " + plainErasure.getIndex();
    }

    public com.geekcommune.friendlybackup.erasure.Erasure getPlainErasure() {
        return plainErasure;
    }

    public Basic.Erasure toProto() {
        Basic.Erasure.Builder proto = Basic.Erasure.newBuilder();
        proto.setVersion(1);
        
        //TODO get rid of this; index can be determined by order in the erasure manifest
        proto.setIndex(plainErasure.getIndex());
        proto.setData(ByteString.copyFrom(plainErasure.getErasureContents()));
        
        return proto.build();
    }

    public static Erasure fromProto(Basic.Erasure proto) {
        versionCheck(1, proto.getVersion(), proto);
        
        int index = proto.getIndex();
        byte[] data = proto.getData().toByteArray();
        return new Erasure(new Buffer(data), index);
    }
}
