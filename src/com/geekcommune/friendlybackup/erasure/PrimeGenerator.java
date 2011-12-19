package com.geekcommune.friendlybackup.erasure;

import java.util.ArrayList;
import java.util.List;

public class PrimeGenerator {

    private static List<Integer> primesFound = new ArrayList<Integer>();
    private int candidate = 1;

    public int nextPrime() {

        nextCandidate:
        while(true) {
            ++candidate;
            int sqrtCandidate = (int) Math.sqrt(candidate);
            
            synchronized(primesFound) {
                for(int primeDivisor : primesFound) {
                    if( primeDivisor > sqrtCandidate ) {
                        primesFound.add(candidate);
                        return candidate;
                    } else if( candidate % primeDivisor == 0 ) {
                        continue nextCandidate;
                    }
                }
                
                primesFound.add(candidate);
            }

            return candidate;
        }
    }

}
