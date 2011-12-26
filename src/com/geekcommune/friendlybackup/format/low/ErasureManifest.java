package com.geekcommune.friendlybackup.format.low;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.geekcommune.communication.RemoteNodeHandle;
import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;
import com.geekcommune.friendlybackup.proto.Basic.ErasureManifest.FetchInfo;
import com.geekcommune.util.Pair;

/**
 * Use Google Protocols
 * @author bobbym
 *
 */
public class ErasureManifest extends BaseData<Basic.ErasureManifest> {
	private List<Pair<HashIdentifier, RemoteNodeHandle>> erasureFetchList =
	        new ArrayList<Pair<HashIdentifier, RemoteNodeHandle>>();
    private int contentSize;
    private int erasuresNeeded;
    private int totalErasures;

    public ErasureManifest() {
    }

	public void add(HashIdentifier erasureId, RemoteNodeHandle storingNode) {
		erasureFetchList.add(new Pair<HashIdentifier, RemoteNodeHandle>(erasureId, storingNode));
		
	}

    public List<Pair<HashIdentifier, RemoteNodeHandle>> getRetrievalData() {
        return Collections.unmodifiableList(erasureFetchList);
    }

    public int getContentSize() {
        return contentSize;
    }

    public void setContentSize(int length) {
        contentSize = length;
    }

    public int getErasuresNeeded() {
        return erasuresNeeded;
    }

    public int getTotalErasures() {
        return totalErasures;
    }

    public void setErasuresNeeded(int erasuresNeeded) {
        this.erasuresNeeded = erasuresNeeded;
    }

    public void setTotalErasures(int totalErasures) {
        this.totalErasures = totalErasures;
    }

    public int getIndex(HashIdentifier hashID) {
        //walk the list of erasures & find the one with this hashID, then return its index
        int idx = -1;
        int i = 0;
        for(Pair<HashIdentifier, RemoteNodeHandle> fetchData : erasureFetchList) {
            if( fetchData.getFirst().equals(hashID) ) {
                idx = i;
                break;
            }
            ++i;
        }
        
        return idx;
    }

    public Basic.ErasureManifest toProto() {
        Basic.ErasureManifest.Builder proto = Basic.ErasureManifest.newBuilder();
        proto.setContentSize(contentSize);
        proto.setErasuresNeeded(erasuresNeeded);
        proto.setTotalErasures(totalErasures);
        
        Map<RemoteNodeHandle, Integer> rnhIdx = new HashMap<RemoteNodeHandle, Integer>();
        for(Pair<HashIdentifier, RemoteNodeHandle> fetchInfo : erasureFetchList) {
            RemoteNodeHandle rnh = fetchInfo.getSecond();
            if( rnhIdx.get(rnh) == null ) {
                proto.addStoringNode(rnh.toProto());
                rnhIdx.put(rnh, proto.getStoringNodeCount() - 1);
            }
        }
        
        for(Pair<HashIdentifier, RemoteNodeHandle> fetchInfo : erasureFetchList) {
            Basic.ErasureManifest.FetchInfo.Builder fiBuilder = Basic.ErasureManifest.FetchInfo.newBuilder();
            fiBuilder.setErasureId(fetchInfo.getFirst().toProto());
            if( rnhIdx.get(fetchInfo.getSecond()) == null ) {
                throw new NullPointerException("Error building rnhIdx");
            }
            fiBuilder.setStoringNodeIndex(rnhIdx.get(fetchInfo.getSecond()));
            
            proto.addFetchInfo(fiBuilder.build());
        }

        return proto.build();
    }

    public static ErasureManifest fromProto(Basic.ErasureManifest proto) throws UnknownHostException {
        versionCheck(1, proto.getVersion(), proto);
        ErasureManifest retval = new ErasureManifest();
        retval.setContentSize(proto.getContentSize());
        retval.setErasuresNeeded(proto.getErasuresNeeded());
        retval.setTotalErasures(proto.getTotalErasures());
        
        List<RemoteNodeHandle> rnhs = new ArrayList<RemoteNodeHandle>();
        for(Basic.RemoteNodeHandle rnh : proto.getStoringNodeList()) {
            rnhs.add(RemoteNodeHandle.fromProto(rnh));
        }
        
        for(FetchInfo fetchInfo : proto.getFetchInfoList()) {
            HashIdentifier erasureId =
                    HashIdentifier.fromProto(fetchInfo.getErasureId());
            RemoteNodeHandle storingNode =
                    rnhs.get(fetchInfo.getStoringNodeIndex());
            retval.add(erasureId, storingNode);
        }
        
        return retval;
    }

}
