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
package com.avaje.ebean.server.lib.sql;

import java.io.Serializable;
import java.sql.*;

/**
 * Meta data about a database column.
 */
public class ColumnInfo implements Serializable {

    static final long serialVersionUID = -5727921165784116926L;

    /**
     * true when an imported foreign key.
     */
    boolean isForeignKey = false;

    /**
     * The index position in the primary key.
     */
    int pkPosition = -1;

    /**
     * Return true if column allows null. Assume nulls are allowed.
     */
    boolean isNullable = true;

    /**
     * String value returned from meta data.
     */
    String nullableCode;

    /**
     * Default value from the meta data.
     */
    String defaultValue;

    int numPrecRadix;

    int decimalDigits;

    int columnSize;

    int dataType;

    int ordinalPosition;

    String columnName;

    String remarks;

    String scopeCatalog;

    String scopeSchema;

    String scopeTable;

    short sourceDataType;

    /**
     * Create the ColumnInfo.
     */
    public ColumnInfo(ResultSet rset) throws SQLException {

        ResultSetMetaData meta = rset.getMetaData();
        int columnCount = meta.getColumnCount();

        this.columnName = rset.getString(4);
        this.dataType = rset.getInt(5);
        this.columnSize = rset.getInt(7);
        this.decimalDigits = rset.getInt(9);
        this.numPrecRadix = rset.getInt(10);
        this.remarks = rset.getString(12);
        this.defaultValue = rset.getString(13);
        this.ordinalPosition = rset.getInt(17);
        this.nullableCode = rset.getString(18);

        if (columnCount >= 22) {
            this.scopeCatalog = rset.getString(19);
            this.scopeSchema = rset.getString(20);
            this.scopeTable = rset.getString(21);
            this.sourceDataType = rset.getShort(22);
        }

        if ("NO".equalsIgnoreCase(nullableCode)) {
            // This has a NOT NULL constraint
            isNullable = false;
        }
    }

    public String toString() {
        return columnName;
    }

    /**
     * Set the primary key position.
     */
    protected void setPrimaryKeyPosition(int pkPosition) {
        this.pkPosition = pkPosition;
    }

    /**
     * Return true if this column is part of the primary key.
     */
    public boolean isPrimaryKey() {
        return pkPosition > -1;
    }

    /**
     * Set when imported foreign keys are found.
     */
    protected void setForeignKey(boolean isForeignKeyColumn) {
        this.isForeignKey = isForeignKeyColumn;
    }

    /**
     * Return the imported foreign key information in ReferenceColumn.
     */
    public boolean isForeignKey() {
        return isForeignKey;
    }

    /**
     * Catalog of table that is the scope of a reference attribute. null if
     * DATA_TYPE isn't REF.
     */
    public String getScopeCatalog() {
        return scopeCatalog;
    }

    /**
     * Schema of table that is the scope of a reference attribute. Null if the
     * DATA_TYPE isn't REF.
     */
    public String getScopeSchema() {
        return scopeSchema;
    }

    /**
     * table name that this the scope of a reference attribute (null if the
     * DATA_TYPE isn't REF).
     */
    public String getScopeTable() {
        return scopeTable;
    }

    /**
     * source type of a distinct type or user-generated Ref type. SQL type from
     * java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF).
     */
    public short getSourceDataType() {
        return sourceDataType;
    }

    /**
     * index of column in table (starting at 1).
     */
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    /**
     * Comment describing column (may be null).
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * Return the db column name.
     */
    public String getName() {
        return columnName;
    }

    /**
     * Return the datatype of the column.
     */
    public int getDataType() {
        return dataType;
    }

    /**
     * Return the column size.
     */
    public int getColumnSize() {
        return columnSize;
    }

    /**
     * Return the decimal digits.
     */
    public int getDecimalDigits() {
        return decimalDigits;
    }
    
    /**
     * Return the number precision radix.
     */
    public int getNumberPrecisionRadix() {
    	return numPrecRadix;
    }

    /**
     * Return the default value.
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * String describing the nullability of this column. Values are YES, NO and
     * null.
     */
    public String getNullableCode() {
        return nullableCode;
    }

    /**
     * Returns true if the column allows Null values.
     */
    public boolean isNullable() {
        return isNullable;
    }

}
