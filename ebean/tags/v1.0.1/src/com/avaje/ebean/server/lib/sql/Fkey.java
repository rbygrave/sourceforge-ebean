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
import java.util.ArrayList;

/**
 * Represents one end of an imported or exported foreign key.
 */
public class Fkey implements Serializable {

    static final long serialVersionUID = 1038321429523113176L;
    
    /**
     * True if this is an exported key. Otherwise imported.
     */
    boolean exported;

    /**
     * True if this is also a primary key.
     */
    boolean primaryKey;
    
    /**
     * True if it is an imported fkey and maps to a unique constraint.
     */
    boolean importUnique;
    
    /**
     * the columns in this imported fkey are nullable.
     */
    boolean importNullable;
    
    /**
     * The foreign key name.
     */
    String fkName;

    /**
     * The table name.
     */
    String tableName;

    FkeyColumn[] columns;
    
    /**
     * List of ReferenceColumn's.
     */
    ArrayList<FkeyColumn> columnList = new ArrayList<FkeyColumn>();
    
    public Fkey(String fkName, String tableName, boolean isExported) {
        this.fkName = fkName;
        this.tableName = tableName;
        this.exported = isExported;
    }

    /**
     * Return the foreign key name.
     */
    public String getFkName() {
        return fkName;
    }

    /**
     * Return the table name.
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Return true if this is an exported key.
     */
    public boolean isExported() {
        return exported;
    }
    
    /**
     * return true if this maps to the primary key.
     */
    public boolean isPrimaryKey(){
    	return primaryKey;
    }
    
    /**
     * This is an imported foreign key on a primary key column.
     */
    protected void setPrimaryKey(boolean primaryKey){
    	this.primaryKey = primaryKey;
    }

    /**
     * Return true if this maps to a unique constraint.
     */
    public boolean isImportUnique() {
		return importUnique;
	}

    /**
     * Set true to indicate this maps to a unique constraint.
     */
	protected void setImportUnique(boolean unique) {
		this.importUnique = unique;
	}

	/**
	 * Return true if one of the imported columns is nullable.
	 */
	public boolean isImportNullable() {
		return importNullable;
	}

	/**
	 * At least one of the imported columns is nullable.
	 */
	protected void setImportNullable(boolean importNullable) {
		this.importNullable = importNullable;
	}

	/**
     * Return the columns involved in the foreign key.
     */
    public FkeyColumn[] columns() {
    	if (columns == null){
    		columns = (FkeyColumn[])columnList.toArray(new FkeyColumn[columnList.size()]);
    		columnList = null;
    	}
        return columns;
    }

    /**
     * Add a column that is part of the foreign key.
     */
    protected void addColumn(FkeyColumn col) {
    	columnList.add(col);
    }

    /**
     * Return true if the otherKey is the inverse of this.
     * <p>
     * That is, each of its primary key columns will match a foreign key column
     * in this Fkey.
     * </p>
     */
    public boolean matchInverse(Fkey otherKey){
    	FkeyColumn[] otherCols = otherKey.columns();
    	FkeyColumn[] cols = columns();
    	if (cols.length != otherCols.length){
    		return false;
    	}
    	for (int i = 0; i < otherCols.length; i++) {
    		String fkCol = otherCols[i].getFkColumnName();
    		if (!containsFkColumn(fkCol)) {
    			return false;
    		}
		}
    	
    	return true;
    }
    
    /**
     * Return true if this fk uses this column.
     * <p>
     * Used when a 2 tables have more than one fk relationship. Used to select
     * the appropriate fk in this case. eg. Customer has a billing and postal
     * address. 2 relationships between customer and address.
     * </p>
     */    
    public boolean containsFkColumn(String columnName) {

        FkeyColumn[] colList = columns();
        for (int i = 0; i < colList.length; i++) {
            FkeyColumn col = (FkeyColumn)colList[i];
            if (columnName.equalsIgnoreCase(col.getFkColumnName())) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (primaryKey){
        	sb.append("PFK ");
        }
        if (exported) {
            sb.append("exported fk");
        } else {
            sb.append("imported fk");
        }
        FkeyColumn[] colList = columns();
        for (int i = 0; i < colList.length; i++) {
            FkeyColumn col = (FkeyColumn) colList[i];
            if (exported) {
                sb.append(" [");
                sb.append(col.getPkColumnName());
                sb.append(" ");
                sb.append("= ");
                sb.append(col.getFkTableName()).append(".").append(col.getFkColumnName());
                sb.append("]");
            } else {
                sb.append(" [");
                sb.append(col.getFkColumnName());
                sb.append(" ");
                sb.append("= ");
                sb.append(col.getPkTableName()).append(".").append(col.getPkColumnName());
                sb.append("]");
            }
        }
        return sb.toString();
    }
}
