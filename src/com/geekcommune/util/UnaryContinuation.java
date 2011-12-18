package com.geekcommune.util;

/**
 * 
 * @see com.geekcommune.util.Continuation
 * @author bobbym
 *
 * @param <T>
 */
public interface UnaryContinuation<T> {
    public void run(T t);
}
