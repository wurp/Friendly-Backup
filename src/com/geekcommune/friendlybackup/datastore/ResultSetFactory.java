package com.geekcommune.friendlybackup.datastore;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Creates one instance of an object from a single row in a ResultSet.
 * @see DataStore.registerResultSetFactory and DataStore.makeObject
 * @author bobbym
 *
 */
public interface ResultSetFactory {
    Object make(ResultSet rs) throws SQLException;
}
