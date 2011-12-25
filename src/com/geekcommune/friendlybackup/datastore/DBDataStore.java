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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.geekcommune.communication.message.Message;
import com.geekcommune.friendlybackup.format.low.HashIdentifier;
import com.geekcommune.identity.PublicIdentityHandle;
import com.geekcommune.identity.Signature;

public abstract class DBDataStore extends DataStore {
    private static final Logger log = Logger.getLogger(DBDataStore.class);

    private static DBDataStore instance;

    protected Connection conn;

    private String connectString;

    private boolean dbInitChecked;

    private ConcurrentHashMap<Class<?>, ResultSetFactory> resultSetFactories = new ConcurrentHashMap<Class<?>, ResultSetFactory>();

    private ConcurrentHashMap<Class<?>, SQLUpdater> sqlUpdaters = new ConcurrentHashMap<Class<?>, SQLUpdater>();

    public DBDataStore(String connectString) {
        this.connectString = connectString;
    }

    public static DBDataStore instance() {
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

    //TODO needs tests
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

    //TODO needs tests
    public void removeLeases(HashIdentifier id, PublicIdentityHandle owner, boolean includeSoft) throws SQLException {
        String sql = "delete from lease where owner = ?";

        String sqlSuffix = "";
        if( !includeSoft ) {
            sqlSuffix = " and soft = false";
        }
        
        PreparedStatement delete = getConnection().
                prepareStatement(sql + sqlSuffix);

        delete.setString(1, owner.fingerprintString());

        delete.execute();
    }

    //TODO needs tests
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
            String[] dbInitStrings = {
                    "create table chunk(key BINARY(20),       data BLOB, PRIMARY KEY (key));",
                    "create table lease(chunk_key BINARY(20), expiry TIMESTAMP, owner VARCHAR(50), soft BOOLEAN, PRIMARY KEY (chunk_key, expiry, owner, soft));",
                    "create table message(transaction_id BINARY(20), type VARCHAR(50) PRIMARY KEY (transaction_id));" //TODO
                    };
            for(String dbInitString : dbInitStrings) {
                PreparedStatement stmt = conn.prepareStatement(dbInitString);
                System.out.println(stmt.execute());
            }
        }

        dbInitChecked  = true;
      }
    }

    public String getDbConnectString() {
      return connectString;
    }

    public static void setInstance(DBDataStore dataStore) {
        instance = dataStore;
    }

    public void deleteMessagesOfType(String type) throws SQLException {
        String sql = "delete from message where type = ?";

        PreparedStatement delete = getConnection().
                prepareStatement(sql);

        delete.setString(1, type);

        delete.execute();
    }

    public List<Message> getMessagesByType(String type) throws ClassNotFoundException, SQLException {
        //retrieve content from db
        PreparedStatement select = getConnection().
          prepareStatement("select transaction_id, clazz, num_tries, destination, ... from message where type = ?");
        select.setString(1, type);
        select.execute();

        List<Message> retval = new ArrayList<Message>();
        
        ResultSet rs = select.getResultSet();
        while( rs.next() ) {
            Message msg = (Message)makeObject(Class.forName(rs.getString("clazz")), rs);
            retval.add(msg);
            log.info("Found " + msg + " of type " + type);
        }

        if( retval.size() == 0 ) {
            log.info("Found no messages for " + type);
        }

        return retval;
    }

    protected Object makeObject(Class<?> clazz, ResultSet rs) throws SQLException {
        return resultSetFactories.get(clazz).make(rs);
    }
    
    /**
     * Register a factory to be used to make instances of a particular class from db results.
     * @param clazz
     * @param rsf
     */
    public void registerResultSetFactory(Class<?> clazz, ResultSetFactory rsf) {
        resultSetFactories.put(clazz, rsf);
    }

    public void updateObject(Object obj) throws SQLException {
        SQLUpdater updater = sqlUpdaters.get(obj.getClass());
        
        //update object in db
        PreparedStatement updateStmt = getConnection().
          prepareStatement(updater.getUpdateSQL());
        updater.setData(updateStmt, obj);

        updateStmt.execute();
        log.info("Updated " + obj);
    }
}
