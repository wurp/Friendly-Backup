package com.geekcommune.util;

import java.util.ArrayList;
import java.util.List;


public class Pair<T1, T2> {

    private T1 t1;
    private T2 t2;

    public Pair(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getFirst() {
        return t1;
    }

    public T2 getSecond() {
        return t2;
    }

    public static <T1, T2> List<T1> firstList(
            List<Pair<T1, T2>> retrievalData) {
        List<T1> retval = new ArrayList<T1>(retrievalData.size());
        
        for(Pair<T1, T2> pair : retrievalData) {
            retval.add(pair.getFirst());
        }
        
        return retval;
    }
    
}
