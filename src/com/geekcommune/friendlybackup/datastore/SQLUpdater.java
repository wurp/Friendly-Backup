package com.geekcommune.friendlybackup.datastore;

import java.sql.PreparedStatement;

public interface SQLUpdater {

    /**
     * Returns the full SQL to update a row in the table managed by this SQL updater.
     * @param obj
     * @return
     */
    String getUpdateSQL();

    /**
     * Sets the fields for the given object obj, to be used to
     * execute the sql returned by getUpdateSQL.
     * @param update
     * @param obj
     */
    void setData(PreparedStatement update, Object obj);

    /**
     * @return whatever object represents the primary key for the table managed by this SQL Updater
     */
    Object getIdentity(Object obj);
}
