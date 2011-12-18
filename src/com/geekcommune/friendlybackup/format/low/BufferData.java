package com.geekcommune.friendlybackup.format.low;

import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.friendlybackup.proto.Basic.Erasure;
import com.google.protobuf.ByteString;
import com.onionnetworks.util.Buffer;

public class BufferData extends BaseData<Basic.Erasure> implements HasHashID {

    private byte[] data;
    private int index;

    public BufferData(Buffer buffer, int i) {
        data = buffer.getBytes();
        index = i;
    }

    public HashIdentifier getHashID() {
        return HashIdentifier.hashForBytes(toProto().toByteArray());
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public byte[] getData() {
        return data;
    }

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

    public static BufferData fromProto(Erasure proto) {
        versionCheck(1, proto.getVersion(), proto);
        
        int index = proto.getIndex();
        byte[] data = proto.getData().toByteArray();
        return new BufferData(new Buffer(data), index);
    }
}
