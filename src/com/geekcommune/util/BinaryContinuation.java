package com.geekcommune.util;

/**
 * 
 * @see com.geekcommune.util.Continuation
 * @author bobbym
 *
 * @param <T>
 */
public interface BinaryContinuation<T1, T2> {
    public void run(T1 t1, T2 t2);
}
