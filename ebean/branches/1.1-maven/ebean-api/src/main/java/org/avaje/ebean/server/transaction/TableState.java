/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package org.avaje.ebean.server.transaction;

import java.util.Date;

/**
 * Maintains summary information about a given table.
 * 
 * <p>
 * Specifically the times when the last insert, update or delete occured against
 * this table. This information is used to determine whether table dependant
 * objects are still valid.
 * </p>
 */
public class TableState {


    int insertCount = 0;

    int updateCount = 0;

    int deleteCount = 0;

    long insertTime = 0;

    long updateTime = 0;

    long deleteTime = 0;

    /**
     * The table name.
     */
    String tableName;

    Object monitor = new Object();
    
    /**
     * Create the TableState.
     */
    protected TableState(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Return true if the table has been modified after since.
     * 
     * @param since the time used to see if a modification occured
     * @param includeInsert if false then just check updates and deletes only
     */
    public boolean isModified(long since, boolean includeInsert) {
        if (updateTime > since) {
            return true;
        }
        if (deleteTime > since) {
            return true;
        }
        if (includeInsert && insertTime > since) {
            return true;
        }
        return false;
    }

    /**
     * Update the table state with the given TableEvent information. This will
     * update the counts and times as appropriate.
     */
    protected void update(TableModInfo tableEvent) {
        synchronized (monitor) {
            // add 1 millisec for bug where time is not accurate enough
            long time = System.currentTimeMillis()+1;
            if (tableEvent.getInsertCount() > 0) {
                insertCount = insertCount + tableEvent.getInsertCount();
                insertTime = time;
            }
            if (tableEvent.getUpdateCount() > 0) {
                updateCount = updateCount + tableEvent.getUpdateCount();
                updateTime = time;
            }
            if (tableEvent.getDeleteCount() > 0) {
                deleteCount = deleteCount + tableEvent.getUpdateCount();
                deleteTime = time;
            }
        }
    }

    /**
     * Set the insert update and delete times to all be now. This has the effect
     * of making any current time dependant objects invalid. Such as dependant
     * lookups or cached entities.
     */
    protected void setModifiedNow() {
        synchronized (monitor) {
            long now = System.currentTimeMillis()+1;
            insertTime = now;
            updateTime = now;
            deleteTime = now;
        }
    }

    /**
     * Return the table name.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Return the time of the last insert.
     */
    public long getLastInsertTime() {
        return insertTime;
    }

    /**
     * Return the time of the last update.
     */
    public long getLastUpdateTime() {
        return updateTime;
    }

    /**
     * Return the time of the last delete.
     */
    public long getLastDeleteTime() {
        return deleteTime;
    }

    /**
     * Return the total number of beans inserted.
     */
    public int getInsertCount() {
        return insertCount;
    }

    /**
     * Return the total number of beans updated.
     */
    public int getUpdateCount() {
        return updateCount;
    }

    /**
     * Return the total number of beans deleted.
     */
    public int getDeleteCount() {
        return deleteCount;
    }

    /**
     * Reset each of the counts back to zero.
     */
    public void resetCounts() {
        insertCount = 0;
        updateCount = 0;
        deleteCount = 0;
    }

    public String toString() {
        return "table:" + tableName + " update:" + new Date(updateTime);
    }

}
