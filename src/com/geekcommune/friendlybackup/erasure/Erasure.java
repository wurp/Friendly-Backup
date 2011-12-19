package com.geekcommune.friendlybackup.erasure;

import com.onionnetworks.util.Buffer;

public class Erasure {

    private byte[] data;
    private int index;

    public Erasure(byte[] data, int index) {
        this.data = data;
        this.index = index;
    }

    public Erasure(Buffer buffer, int i) {
        this(buffer.getBytes(), i);
    }

    public int getIndex() {
        return index;
    }

    public byte[] getErasureContents() {
        return data;
    }

    public void setIndex(int index) {
        this.index = index;
    }

}
