package com.geekcommune.friendlybackup.erasure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.geekcommune.util.Pair;
import com.onionnetworks.fec.FECCode;
import com.onionnetworks.fec.FECCodeFactory;
import com.onionnetworks.util.Buffer;

public class ErasureUtil {
    private static final Logger log = Logger.getLogger(ErasureUtil.class);
    private static Map<Pair<Integer,Integer>, FECCode> fecCodeMap = new HashMap<Pair<Integer, Integer>, FECCode>();;

    /**
     * Creates 'erasures', blocks of data that can be used to reconstitute the original data.
     * 
     * Pass in raw data, the number of erasure blocks needed to reconstitute, and the total
     * number of erasures to create.  Returns an array of Buffers containing the erasure blocks.
     * 
     * @param data
     * @param erasuresNeeded
     * @param totalErasures
     * @return
     */
	public static Buffer[] encode(byte[] data, int erasuresNeeded, int totalErasures) {
		int erasureSize = calcErasureBufferLength(erasuresNeeded, data.length);
		
		//We have to put the input data into Buffers for FEC to process it.
		//Buffer reuses, rather than copies, the data in the byte[]
        Buffer[] sourceBuffers = new Buffer[erasuresNeeded];

        for (int i = 0; i < sourceBuffers.length; i++) {
            try {
                //unless data.length is a multiple of 60, at some point there's not enough data left to 
                //fill the buckets we're trying to fill
                if( (i + 1) * erasureSize <= data.length ) {
                    sourceBuffers[i] = new Buffer(data, i*erasureSize, erasureSize);
                } else {
                    //fill out the beginning of helperBuff with the end of data (if any remaining)
                    //otherwise, leave it as 0s
                    byte[] helperBuff = new byte[erasureSize];
                    if( i * erasureSize < data.length ) {
                        System.arraycopy(data, i * erasureSize, helperBuff, 0, data.length - i * erasureSize);
                    }
                    sourceBuffers[i] = new Buffer(helperBuff, 0, erasureSize);
                }
            } catch(RuntimeException e) {
                log.error("Error creating buffer " + i);
                throw e;
            }
        }

        //The byte[] to receive the calculated erasure data
        int numRepairBlocks = totalErasures - erasuresNeeded;
        byte[] erasureData = new byte[erasureSize * numRepairBlocks];
        //Once again, we need to use Buffers for FEC to put the data into 'repair'
        Buffer[] repairBuffers = new Buffer[numRepairBlocks];
        
        for (int i = 0; i < repairBuffers.length; i++) {
            repairBuffers[i] = new Buffer(erasureData, i*erasureSize, erasureSize);
        }

        //Tell which erasures to create.  For now, we create all of them in one swoop.
        //The first 'erasuresNeeded' erasures are just the original 'data' content,
        //so we only create the last numRepairBlocks erasures.
        int[] repairIndex = new int[repairBuffers.length];

        for (int i = 0; i < repairIndex.length; i++) {
            repairIndex[i] = i + erasuresNeeded;
        }

        //create our fec code
        FECCode fec = getFECCode(erasuresNeeded, totalErasures);

        //encode the data
        fec.encode(sourceBuffers, repairBuffers, repairIndex);
        //encoded data is now contained in the repairBuffer/repair byte array

        //create an array of buffers that points to all the data (original + repair blocks)
        Buffer[] allErasureBuffers = new Buffer[sourceBuffers.length + repairBuffers.length];
        System.arraycopy(sourceBuffers, 0, allErasureBuffers, 0, sourceBuffers.length);
        System.arraycopy(repairBuffers, 0, allErasureBuffers, sourceBuffers.length, repairBuffers.length);
        
		return allErasureBuffers;
	}

    private static synchronized FECCode getFECCode(int erasuresNeeded, int totalErasures) {
        Pair<Integer,Integer> key = new Pair<Integer,Integer>(erasuresNeeded,totalErasures);
        FECCode retval = fecCodeMap.get(key);
        if( retval == null ) {
            retval = FECCodeFactory.getDefault().createFECCode(erasuresNeeded,totalErasures);
            fecCodeMap.put(key, retval);
        }
        
        return retval;
    }

    private static int calcErasureBufferLength(int erasuresNeeded,
            int dataLength) {
        int erasureSize = dataLength / erasuresNeeded;
		if( dataLength % erasuresNeeded != 0 ) {
		    erasureSize += 1;
		}
		
		return erasureSize;
    }

    /**
     * Decodes 'erasures', blocks of data that can be used to reconstitute the original data.
     * If you hand it a large enough number ('erasuresNeeded') of erasure blocks, it will
     * fill 'result' with the original data.
     * 
     * @param result - the algorithm puts the decoded data here
     * @param erasuresNeeded - number of erasures needed to reconstitute the result
     * @param totalErasures - total number of erasures built originally
     * @param erasures - the blocks containing the data from which to rebuild
     * @return
     */
    public static void decode(byte[] result, int erasuresNeeded,
            int totalErasures, List<Erasure> erasures) {
        decode(result, erasuresNeeded, totalErasures, erasures, Safety.SAFE);
    }
    
    public static enum Safety { SAFE, UNSAFE };
    
    public static void decode(byte[] result, int erasuresNeeded,
            int totalErasures, List<Erasure> erasures, Safety safety) {
        //TODO if BufferData in erasures already pointed into result, this would use half as much memory...
        
        if( safety == Safety.SAFE && erasures.size() < erasuresNeeded) {
            throw new RuntimeException("Need " + erasuresNeeded + " blocks to rebuild data; found " + erasures.size());
        }
        
        int packetsize = calcErasureBufferLength(erasuresNeeded, result.length);

        //build a set of buffers to store the result in.  The last buffer is special;
        //it could be padded.  We'll use a separate temp byte[], then copy the data
        //into result at the end.
        Buffer[] receiverBuffer = new Buffer[erasuresNeeded];
        FakeBuffer[] fakeBuffers = new FakeBuffer[receiverBuffer.length];
        byte[] lastReceiver = null;
        int lastReceiverIndex = -1;
        for(int i = 0; i < receiverBuffer.length; ++i) {
            if( (i + 1) * packetsize <= result.length ) {
                receiverBuffer[i] = new Buffer(result, i*packetsize, packetsize);
                fakeBuffers[i] = new FakeBuffer(result, i*packetsize);
            } else {
                byte[] helperBuff = new byte[packetsize];
                if( lastReceiver == null ) {
                    lastReceiver = helperBuff;
                    lastReceiverIndex = i;
                }
                receiverBuffer[i] = new Buffer(helperBuff, 0, packetsize);
                fakeBuffers[i] = new FakeBuffer(helperBuff, 0);
            }
        }

        //We only need to store k, packets received
        //Don't forget we need the index value for each packet too
        int[] receiverIndex = new int[erasuresNeeded];

        //copy the data from the BufferData into the byte[]s for our buffers
        for (int i = 0; i < erasuresNeeded; i++) {
            Erasure erasure = erasures.get(i);
            
            receiverIndex[i] = erasure.getIndex();
            System.arraycopy(erasure.getErasureContents(), 0, fakeBuffers[i].data, fakeBuffers[i].startIndex, packetsize);
        }
        
        //finally we can decode, which for all buffers that receive data but the last will write to result
        //the last buffer that gets data in it may be spillover to lastReceiver
        FECCode fec = getFECCode(erasuresNeeded, totalErasures);
        fec.decode(receiverBuffer, receiverIndex);
        
        //copy over that bit of the last block and we're done!
        int lenOfDataAlreadyInResult = lastReceiverIndex * packetsize;
        if( lastReceiver != null ) {
            System.arraycopy(lastReceiver, 0, result, lenOfDataAlreadyInResult, result.length - lenOfDataAlreadyInResult);
        }
    }
    
    private static class FakeBuffer {

        private int startIndex;
        private byte[] data;

        public FakeBuffer(byte[] data, int startIndex) {
            this.data = data;
            this.startIndex = startIndex;
        }
        
    }
}
