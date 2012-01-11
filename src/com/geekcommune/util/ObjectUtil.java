package com.geekcommune.util;

public class ObjectUtil {

    public static boolean equals(Object lhs, Object rhs) {
        return lhs == null ? rhs == null : lhs.equals(rhs);
    }

}
