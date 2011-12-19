package com.geekcommune.util;

public class StringUtil {

    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String hexdump(byte[] data) {
        return hexdump(data, 0, data.length);
    }
    
    public static String hexdump(byte[] data, int start, int length) {
        StringBuilder buff = new StringBuilder(length * 2);
        for(int i = start; i < start + length; ++i) {
            int byteVal = data[i];
            if( byteVal < 0 ) byteVal += 256;
            
            buff.append(HEX[byteVal / 16]);
            buff.append(HEX[byteVal % 16]);
        }

        return buff.toString();
    }

}
