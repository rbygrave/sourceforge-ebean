/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package org.avaje.ebean.server.lib.sql;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A Column that is Referenced in a Foreign key.
 */
public class FkeyColumn implements Serializable {

    static final long serialVersionUID = 6709717034343906703L;
    

    String pkTableCat;

    String pkTableSchema;

    String pkTableName;

    String pkColumnName;

    String fkTableCat;

    String fkTableSchema;

    String fkTableName;

    String fkColumnName;

    int keySeq;

    int updateRule;

    int deleteRule;

    String fkName;

    String pkName;

    int deferablity;

    boolean exported;

    String catalog;

    String schema;

    String tableName;

    /**
     * Create the ReferenceColumn. Refer to java.sql.DatabaseMetaData.
     */
    public FkeyColumn(boolean exported, ResultSet rset) throws SQLException {

        this.exported = exported;
        pkTableCat = rset.getString(1);
        pkTableSchema = rset.getString(2);
        pkTableName = rset.getString(3);
        pkColumnName = rset.getString(4);
        fkTableCat = rset.getString(5);
        fkTableSchema = rset.getString(6);
        fkTableName = rset.getString(7);
        fkColumnName = rset.getString(8);
        keySeq = rset.getInt(9);
        updateRule = rset.getInt(10);
        deleteRule = rset.getInt(11);
        fkName = rset.getString(12);
        pkName = rset.getString(13);
        deferablity = rset.getInt(14);

        if (exported) {
            catalog = fkTableCat;
            schema = fkTableSchema;
            tableName = fkTableName;
        } else {
            catalog = pkTableCat;
            schema = pkTableSchema;
            tableName = pkTableName;
        }
    }

	/**
     * Return the name of the foreign key.
     */
    public String getFkName() {
        return fkName;
    }

    /**
     * Return the catalog.
     */
    public String getCatalog() {
        return catalog;
    }

    /**
     * Return the fk deferablity.
     */
    public int getDeferablity() {
        return deferablity;
    }

    /**
     * Return the delete rule.
     */
    public int getDeleteRule() {
        return deleteRule;
    }

    /**
     * Return true if this is an exported key.
     */
    public boolean isExported() {
        return exported;
    }

    /**
     * Return the foreign key column.
     */
    public String getFkColumnName() {
        return fkColumnName;
    }

    /**
     * Return the foreign key table catalog.
     */
    public String getFkTableCat() {
        return fkTableCat;
    }

    /**
     * Return the foreign key table.
     */
    public String getFkTableName() {
        return fkTableName;
    }

    /**
     * Return the foreign key schema.
     */
    public String getFkTableSchema() {
        return fkTableSchema;
    }

    /**
     * Return the position of the column in the foreign key.
     */
    public int getKeySeq() {
        return keySeq;
    }

    /**
     * Return the primary key column.
     */
    public String getPkColumnName() {
        return pkColumnName;
    }

    /**
     * Return the descriptive name of the primary key.
     */
    public String getPkName() {
        return pkName;
    }

    /**
     * Return the primary key catalog.
     */
    public String getPkTableCat() {
        return pkTableCat;
    }

    /**
     * Return the primary key table.
     */
    public String getPkTableName() {
        return pkTableName;
    }

    /**
     * Return the primary key schema.
     */
    public String getPkTableSchema() {
        return pkTableSchema;
    }

    /**
     * Return the schema for this column.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Return the table for this column.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Return the update rule.
     */
    public int getUpdateRule() {
        return updateRule;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("pk[").append(pkTableName).append(".");
        sb.append(pkColumnName).append("] fk[").append(fkTableName);
        sb.append(".").append(fkColumnName).append("]");
        return sb.toString();
    }

}
