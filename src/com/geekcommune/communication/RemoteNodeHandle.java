package com.geekcommune.communication;

import java.net.InetAddress;

import com.geekcommune.friendlybackup.format.BaseData;
import com.geekcommune.friendlybackup.proto.Basic;

public class RemoteNodeHandle extends BaseData<Basic.RemoteNodeHandle> {

    private String name;
    private String email;
    private String connectString;

    public RemoteNodeHandle(String name, String email, String connectString) {
        this.name = name;
        this.email = email;
        this.connectString = connectString;
    }

    public Basic.RemoteNodeHandle toProto() {
        Basic.RemoteNodeHandle.Builder bldr = Basic.RemoteNodeHandle.newBuilder();
        bldr.setConnectString(connectString);
        bldr.setEmail(email);
        bldr.setName(name);
        bldr.setVersion(1);
        
        return bldr.build();
    }

    @Override
    public boolean equals(Object obj) {
        if( obj instanceof RemoteNodeHandle ) {
            RemoteNodeHandle rhs = (RemoteNodeHandle) obj;
            return name.equals(rhs.name) && email.equals(rhs.email) && connectString.equals(rhs.connectString);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (name + "~" + email + "~" + connectString).hashCode();
    }
    
    @Override
    public String toString() {
        return "RemoteNodeHandle(" + name + ", " + email + ", " + connectString + ")"; 
    }
    
    public static RemoteNodeHandle fromProto(Basic.RemoteNodeHandle proto) {
        versionCheck(1, proto.getVersion(), proto);
        
        String name = proto.getName();
        String email = proto.getEmail();
        String connectString = proto.getConnectString();
        
        return new RemoteNodeHandle(name, email, connectString);
    }

    public InetAddress getAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String getName() {
        return name;
    }
}
