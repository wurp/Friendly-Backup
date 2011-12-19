package com.geekcommune.friendlybackup.erasurefinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.bouncycastle.util.Arrays;

import com.geekcommune.friendlybackup.format.low.Erasure;
import com.onionnetworks.util.Buffer;

public class ErasureUtilTest extends TestCase {
    public void testPrime() {
        PrimeGenerator pg = new PrimeGenerator();
        for(int i = 0; i < 15; ++i) {
            System.out.println(pg.nextPrime());
        }
    }
    
    public void testSmall() throws Exception {
        int[] sizes = {0, 1, 59, 60, 61, 3599, 3600, 3601 };
        for(int i = 0; i < sizes.length; ++i) {
            byte[] input = new byte[sizes[i]];
            simplePopulate(input);
            encodeDecodeVerify(input);
        }
    }
    
    private interface BufferKiller {
        public boolean shouldKill(int i, int numBuffers);
    }

    public void testMissingErasures() throws Exception {
        int[] sizes = {0, 1, 59, 60, 61, 3599, 3600, 3601, 60 * 1024, 100 * 1024, 60 * 1024 - 1 };
        for(int i = 0; i < sizes.length; ++i) {
            byte[] input = new byte[sizes[i]];
            simplePopulate(input);
            encodeDecodeVerify(input, new BufferKiller() {
                public boolean shouldKill(int i, int numBuffers) {
                    return i % 3 == 2;
                }
            });
        }
        for(int i = 0; i < sizes.length; ++i) {
            byte[] input = new byte[sizes[i]];
            simplePopulate(input);
            encodeDecodeVerify(input, new BufferKiller() {
                int numKilled;
                public boolean shouldKill(int i, int numBuffers) {
                    boolean kill = numKilled < numBuffers / 3 && Math.random() < 0.35;
                    if( kill ) {
                        ++numKilled;
                    }

                    return kill;
                }
            });
        }
        for(int i = 0; i < sizes.length; ++i) {
            byte[] input = new byte[sizes[i]];
            simplePopulate(input);
            encodeDecodeVerify(input, new BufferKiller() {
                public boolean shouldKill(int i, int numBuffers) {
                    return i % 3 == 2;
                }
            });
        }
    }

    public enum Order { IN_ORDER, SCRAMBLE };
    
    public void testScrambleOrder() throws Exception {
        int[] sizes = {0, 1, 59, 60, 61, 3599, 3600, 3601, 60 * 1024, 100 * 1024, 60 * 1024 - 1 };
        for(int i = 0; i < sizes.length; ++i) {
            byte[] input = new byte[sizes[i]];
            simplePopulate(input);
            encodeDecodeVerify(
                    input,
                    new BufferKiller() {
                        public boolean shouldKill(int i, int numBuffers) {
                            return i % 3 == 2;
                        }
                    },
                    Order.SCRAMBLE);
        }
        for(int i = 0; i < sizes.length; ++i) {
            byte[] input = new byte[sizes[i]];
            simplePopulate(input);
            encodeDecodeVerify(input, new BufferKiller() {
                int numKilled;
                public boolean shouldKill(int i, int numBuffers) {
                    boolean kill = numKilled < numBuffers / 3 && Math.random() < 0.35;
                    if( kill ) {
                        ++numKilled;
                    }

                    return kill;
                }
            });
        }
        for(int i = 0; i < sizes.length; ++i) {
            byte[] input = new byte[sizes[i]];
            simplePopulate(input);
            encodeDecodeVerify(input, new BufferKiller() {
                public boolean shouldKill(int i, int numBuffers) {
                    return i % 3 == 2;
                }
            });
        }
    }

    public void testMegs() throws Exception {
        int[] sizes = { 100 * 1024, 150 * 1024, 225 * 1024, 375 * 1024, 512 * 1024,
                768 * 1024, 1000 * 1024, 1024 * 1024, 5 * 1024 * 1024, 15 * 1024 * 1024, 30 * 1024 * 1024, 60 * 1024 * 1024 };
        
        for(int i = 0; i < sizes.length; ++i) {
            byte[] input = new byte[sizes[i]];
            simplePopulate(input);
            long start = System.currentTimeMillis();
            encodeDecodeVerify(input);
            System.out.println(sizes[i] + " took " + (System.currentTimeMillis() - start));
        }
    }
    
    //TODO more tests: remove more than 1/3 & verify it fails to reconstruct 

    private void simplePopulate(byte[] input) {
        Random rand = new Random();
        for(int i = 0; i < input.length; ++i) {
            input[i] = (byte) rand.nextInt();
        }
    }

    private void encodeDecodeVerify(byte[] input) {
        final int erasuresNeeded = 40;
        final int totalErasures = 60;
        
        encodeDecodeVerify(erasuresNeeded, totalErasures, input, new BufferKiller() {
            public boolean shouldKill(int i, int numBuffers) {
                return false;
            }
        }, Order.IN_ORDER);
    }

    private void encodeDecodeVerify(byte[] input, BufferKiller bufferKiller) {
        encodeDecodeVerify(input, bufferKiller, Order.IN_ORDER);
    }
    
    private void encodeDecodeVerify(byte[] input, BufferKiller bufferKiller, Order order) {
        final int erasuresNeeded = 40;
        final int totalErasures = 60;
        
        encodeDecodeVerify(erasuresNeeded, totalErasures, input, bufferKiller, order);
    }

    private void encodeDecodeVerify(
            final int erasuresNeeded,
            final int totalErasures,
            byte[] input,
            BufferKiller bufferKiller,
            Order order) {
        Buffer[] erasures = ErasureUtil.encode(input, erasuresNeeded, totalErasures);

        List<Erasure> erasureWrappers = new ArrayList<Erasure>();
        for(int i = 0; i < erasures.length; ++i) {
            if( !bufferKiller.shouldKill(i, erasures.length) ) {
                erasureWrappers.add(new Erasure(erasures[i], i));
            }
        }
        
        if( order == Order.SCRAMBLE ) {
            Collections.sort(erasureWrappers, new Comparator<Erasure>() {
                public int compare(Erasure o1, Erasure o2) {
                    double rand = Math.random();
                    return rand < 0.33 ? -1 : (rand > 0.66 ? 1 : 0);
                }
            });
        }
        
        byte[] result = new byte[input.length];
        ErasureUtil.decode(result, erasuresNeeded, totalErasures, erasureWrappers);
        
        assertTrue("Input data different from output data", Arrays.areEqual(input, result));
    }
}
