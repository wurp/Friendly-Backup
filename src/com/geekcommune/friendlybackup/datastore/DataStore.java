package com.geekcommune.friendlybackup.datastore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.identity.PublicIdentityHandle;
import com.geekcommune.identity.Signature;

public class DataStore {
    private static final Logger log = Logger.getLogger(DataStore.class);

    private static DataStore instance;

    protected Connection conn;

    private String connectString;

    private boolean dbInitChecked;

    public DataStore(String connectString) {
        this.connectString = connectString;
    }

    public static DataStore instance() {
        return instance ;
    }

    /**
     * Return a piece of data that should already be local.
     * @param id
     * @return
     * @throws SQLException 
     */
    public synchronized byte[] getData(HashIdentifier id) throws SQLException {

        //retrieve content from db
        PreparedStatement select = getConnection().
          prepareStatement("select data from chunk where key = ?");
        //TODO make sure that bytes.equals(Id.build(bytes).toByteArray()) for our hash function
        select.setBytes(1, id.getData());
        select.execute();

        byte[] retval = null;
        ResultSet rs = select.getResultSet();
        if( rs.next() ) {
          byte[] content = rs.getBytes(1);

          retval = content;
        }

        log.info("Got " + (retval == null ? null : retval.length) + " bytes for " + id);
        return retval;
    }


    public synchronized void storeData(HashIdentifier id, byte[] data, Lease lease) throws SQLException {
        storeData(id, data, Collections.singletonList(lease));
    }

    public synchronized void storeData(HashIdentifier id, byte[] data, List<Lease> leases) throws SQLException {
        if( getData(id) == null ) {
            //TODO make sure that bytes.equals(Id.build(bytes).toByteArray()) for our hash function

            //insert self into db
            PreparedStatement insert = getConnection().
              prepareStatement("insert into chunk values (?, ?)");
            insert.setBytes(1, id.getData());
            insert.setBytes(2, data);
            insert.execute();
            log.info("Writing " + (data == null ? null : data.length) + " bytes for " + id);
            
            addLeases(id, leases);
        } else {
            log.info("Not writing " + (data == null ? null : data.length) + " bytes duplicate data for " + id);
        }
     }

    public void addLeases(HashIdentifier id, List<Lease> leases) throws SQLException {
        PreparedStatement insert = getConnection().
                prepareStatement("insert into lease values (?, ?, ?, ?)");

        for(Lease lease : leases) {
            insert.setBytes(    1, id.getData());
            insert.setTimestamp(2, new Timestamp(lease.getExpiry().getTime()));
            insert.setString(   3, lease.getOwner().fingerprintString());
            insert.setBoolean(  4, lease.isSoft());
            insert.execute();
            log.info("Writing lease " + lease + " for " + id);
        }
    }

    public List<Lease> getLeases(HashIdentifier id) throws SQLException {
        //retrieve content from db
        PreparedStatement select = getConnection().
          prepareStatement("select chunk_key, owner, expiry, soft from lease where chunk_key = ?");
        select.setBytes(1, id.getData());
        select.execute();

        List<Lease> retval = new ArrayList<Lease>();
        
        ResultSet rs = select.getResultSet();
        while( rs.next() ) {
            Lease lease = new Lease(
                    rs.getDate("expiry"),
                    new PublicIdentityHandle(rs.getString("owner")),
                    new Signature(rs.getString("signature")),
                    rs.getBoolean("soft"));
            retval.add(lease);
            log.info("Found " + lease + " for " + id);
        }

        if( retval.size() == 0 ) {
            log.info("Found no leases for " + id);
        }

        return retval;
    }

    public List<byte[]> getDataList(List<HashIdentifier> ids) throws SQLException {
        List<byte[]> retval = new ArrayList<byte[]>(ids.size());
        
        for(HashIdentifier id : ids) {
            retval.add(getData(id));
        }
        
        return retval;
    }


    @Override
    public String toString() {
      return "DataStore(" + connectString + ")";
    }

    public static Connection makeConnection(String connectString) throws SQLException {
      return DriverManager.getConnection(connectString,
        "SA", "");
    }

    public synchronized Connection getConnection() throws SQLException {
      if( conn == null ) {
        conn = makeConnection(connectString);

        //create the tables if they don't already exist
        dbInit(conn);
        //TODO: connection pooling?  Use only one persistent connection?
        //TODO: shutdown?  shutdown compact?
      }

      return conn;
    }

    public void dbInit(Connection conn) throws SQLException {
      if( !dbInitChecked ) {
        try {
          //we just try to pull data from the table, and if it's not there
          //(throws an exception), we assume that the db hasn't been created
          PreparedStatement stmt = conn.prepareStatement("select key from chunk limit 1");
          stmt.execute();
        } catch( Exception e ) {
            // TODO should use artificial, generated, primary key?
            String dbInitString =
                    "create table chunk(key BINARY(20),       data BLOB, PRIMARY KEY (key));" + 
                    "create table lease(chunk_key BINARY(20), expiry TIMESTAMP, owner VARCHAR, soft BOOLEAN PRIMARY KEY (chunk_key, expiry, owner, soft));";
          PreparedStatement stmt = conn.prepareStatement(dbInitString);
          System.out.println(stmt.execute());
        }

        dbInitChecked  = true;
      }
    }

    public String getDbConnectString() {
      return connectString;
    }

    public static void setInstance(DataStore dataStore) {
        instance = dataStore;
    }
}