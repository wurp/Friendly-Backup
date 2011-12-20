package com.geekcommune.util;

import java.util.Date;

public class DateUtil {

    public static Date oneDayHence() {
        return new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
    }

    public static Date oneHourHence() {
        return new Date(System.currentTimeMillis() + 60 * 60 * 1000);
    }

}
