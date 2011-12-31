package com.geekcommune.identity;

public interface KeyDataSource {

    char[] getPassphrase();

    String getIdentity();

}
