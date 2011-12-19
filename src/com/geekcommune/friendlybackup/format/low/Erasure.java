package com.geekcommune.friendlybackup.format.low;

import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.google.protobuf.ByteString;
import com.onionnetworks.util.Buffer;

public class Erasure extends BaseData<Basic.Erasure> {

    private byte[] data;
    private int index;

    public Erasure(Buffer buffer, int i) {
        data = buffer.getBytes();
        index = i;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public byte[] getErasureContents() {
        return data;
    }

    @Override
    public String toString() {
        return getClass().getName() + ": idx " + index;
    }

    public Basic.Erasure toProto() {
        Basic.Erasure.Builder proto = Basic.Erasure.newBuilder();
        proto.setVersion(1);
        proto.setIndex(index);
        proto.setData(ByteString.copyFrom(data));
        
        return proto.build();
    }

    public static Erasure fromProto(Basic.Erasure proto) {
        versionCheck(1, proto.getVersion(), proto);
        
        int index = proto.getIndex();
        byte[] data = proto.getData().toByteArray();
        return new Erasure(new Buffer(data), index);
    }
}
