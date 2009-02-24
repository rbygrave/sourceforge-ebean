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
package com.avaje.ebean.server.transaction;

import java.io.Serializable;

/**
 * Summary transaction information at a table level. Contained by a
 * TransactionEvent this is used to maintain the TableState held by the
 * TableManager.
 */
public class TableModInfo implements Serializable {

    static final long serialVersionUID = -4892325166314719020L;
    
    /**
     * The table name.
     */
    String tableName;

    /**
     * Total number of rows inserted.
     */
    int insertCount = 0;

    /**
     * Total numer of rows udpdated.
     */
    int updateCount = 0;

    /**
     * Total number of rows deleted.
     */
    int deleteCount = 0;
    
    /**
     * Create a tableEvent for a given table.
     * <p>
     * As a bean is persisted it is passed here, to maintain a count of
     * inserted, updated and deleted entities.
     * </p>
     */
    public TableModInfo(String tableName) {
        this.tableName = tableName;
    }

    /**
     * equal if the tablename is the same.
     */
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if (obj instanceof TableModInfo) {
            TableModInfo mi = (TableModInfo)obj;
            return mi.hashCode() == hashCode();
        }
        return false;
    }

    public int hashCode() {
        int hc = TableModInfo.class.getName().hashCode();
        hc = hc*31 + tableName.hashCode();
        return hc;
    }

    /**
     * Return the name of the table that was inserted, updated, deleted from.
     * Note that this tableName has been converted to uppercase.
     */
    public String getTableName() {
        return tableName;
    }

    public void add(TableModInfo info){
        insertCount += info.insertCount;
        updateCount += info.updateCount;
        deleteCount += info.deleteCount;
    }
    
    /**
     * Increment the insert count.
     */
    public void incrementInsert(int count) {
        insertCount += count;
    }

    /**
     * Increment the update count.
     */
    public void incrementUpdate(int count) {
        updateCount += count;
    }

    /**
     * Increment the delete count.
     */
    public void incrementDelete(int count) {
        deleteCount += count;
    }

    /**
     * The number of rows inserted into this table in this transaction.
     */
    public int getInsertCount() {
        return insertCount;
    }

    /**
     * The number of rows updated in this table in this transaction.
     */
    public int getUpdateCount() {
        return updateCount;
    }

    /**
     * The number of rows deleted from this table in this transaction.
     */
    public int getDeleteCount() {
        return deleteCount;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[TableModInfo tn='").append(tableName).append("' i='").append(insertCount).append(
                "' u='").append(updateCount).append("' d='").append(deleteCount).append("']");
        return sb.toString();
    }

}
