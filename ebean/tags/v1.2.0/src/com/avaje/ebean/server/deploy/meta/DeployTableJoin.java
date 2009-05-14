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
package com.avaje.ebean.server.deploy.meta;

import java.util.ArrayList;

import javax.persistence.JoinColumn;

import com.avaje.ebean.server.deploy.BeanCascadeInfo;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.lib.sql.Fkey;
import com.avaje.ebean.server.lib.sql.FkeyColumn;
import com.avaje.ebean.util.Message;

/**
 * Represents a join to another table during deployment phase.
 * <p>
 * This gets converted into a immutable TableJoin when complete.
 * </p>
 */
public class DeployTableJoin {

    /**
     * Flag set when the imported key maps to the primary key.
     * This occurs for intersection tables (ManyToMany).
     */
    boolean importedPrimaryKey;
    
    /**
     * The joined table.
     */
    String table;

    /**
     * The table alias for the joined table.
     */
    String foreignTableAlias;

    /**
     * The table alias for local base table.
     */
    String localTableAlias;
    
    /**
     * The type of join. LEFT OUTER etc.
     */
    String type = TableJoin.JOIN;

    /**
     * The list of properties mapped to this joined table.
     */
    ArrayList<DeployBeanProperty> properties = new ArrayList<DeployBeanProperty>();

    /**
     * The list of join column pairs. Used to generate the on clause.
     */
    ArrayList<DeployTableJoinColumn> columns = new ArrayList<DeployTableJoinColumn>();

    /**
     * The persist cascade info.
     */
    BeanCascadeInfo cascadeInfo = new BeanCascadeInfo();
    

    /**
     * Create a DeployTableJoin.
     */
    public DeployTableJoin() {
    }
    
    /**
     * If the join has only specified one of the columns, then find and assign the missing one.
     * <p>
     * This will find the PK of the table and assign that to the missing side of the join.
     * </p>
     */
    public void setUndefinedColumnIfRequired(DeployUtil util, String table){
    	if (columns.size() == 1){
    		DeployTableJoinColumn joinColumn = columns.get(0);
    		joinColumn.setUndefinedColumnIfRequired(util, table);
    	}
    }
    
    public String toString() {
        return type + " " + table + " " + columns;
    }

    /**
     * Return true if the imported foreign key maps to the primary key.
     */
    public boolean isImportedPrimaryKey() {
		return importedPrimaryKey;
	}

    /**
     * Flag set when the imported key maps to the primary key.
     * This occurs for intersection tables (ManyToMany).
     */
	public void setImportedPrimaryKey(boolean importedPrimaryKey) {
		this.importedPrimaryKey = importedPrimaryKey;
	}

	/**
     * Return true if the JoinOnPair have been set.
     */
    public boolean hasJoinColumns() {
        return columns.size() > 0;
    }

    /**
     * Return the persist info.
     */
    public BeanCascadeInfo getCascadeInfo() {
        return cascadeInfo;
    }
    
    /**
     * Add columns from DB dictionary foreign key information.
     */
    public void addColumns(Fkey fkey) {
		
    	boolean exported = fkey.isExported();
    	
    	if (fkey.isPrimaryKey()) {
			// foreign key maps to the primary key
			setImportedPrimaryKey(true);
		}
	
    	FkeyColumn[] cols = fkey.columns();
		for (int i = 0; i < cols.length; i++) {
			FkeyColumn col = cols[i];
			
			DeployTableJoinColumn joinColumn;
			if (exported) {
				// its around the other way...
				joinColumn = new DeployTableJoinColumn(col.getPkColumnName(), col.getFkColumnName());
			} else {
				joinColumn = new DeployTableJoinColumn(col.getFkColumnName(), col.getPkColumnName());
			}
	
			addJoinColumn(joinColumn);
		}
		
    	if (!exported){
    		if(fkey.isImportNullable()) {
    			// make this an outer join as some of the columns are nullable
    			setType(TableJoin.LEFT_OUTER);
    		}
    	}
	}
    
    /**
     * Add a join pair
     */
    public void addJoinColumn(DeployTableJoinColumn pair) {
        columns.add(pair);
    }

    /**
     * Add a JoinColumn
     * <p>
     * The order is generally true for OneToMany and false for ManyToOne relationships.
     * </p>
     */
    public void addJoinColumn(boolean order, JoinColumn jc) {
		if (!"".equals(jc.table())) {
			setTable(jc.table());
		}
    	addJoinColumn(new DeployTableJoinColumn(order, jc));
    }
    
    /**
     * Add a JoinColumn array.
     */
    public void addJoinColumn(boolean order, JoinColumn[] jcArray) {
    	for (int i = 0; i < jcArray.length; i++) {
    		addJoinColumn(order, jcArray[i]);
		}
    }
    
    /**
     * Return the join columns.
     */
    public DeployTableJoinColumn[] columns() {
    	return (DeployTableJoinColumn[])columns.toArray(new DeployTableJoinColumn[columns.size()]);
    }

    
    /**
     * For secondary table joins returns the properties mapped to that table.
     */
    public DeployBeanProperty[] properties() {
    	return (DeployBeanProperty[])properties.toArray(new DeployBeanProperty[properties.size()]);
    }

    /**
     * Add a property for this tableJoin.
     */
    public void addProperty(DeployBeanProperty prop) {
        prop.setDbTableAlias(getForeignTableAlias());
        prop.setSecondaryTable();
        properties.add(prop);
    }

    /**
     * Return the table alias used by this join.
     */
    public String getForeignTableAlias() {
        return foreignTableAlias;
    }

    /**
     * Set the table alias used by this join.
     */
    public void setForeignTableAlias(String alias) {
        this.foreignTableAlias = alias;
    }

    /**
     * Return the local base table alias.
     */
    public String getLocalTableAlias() {
		return localTableAlias;
	}

    /**
     * set the local base table alias.
     */
	public void setLocalTableAlias(String localTableAlias) {
		this.localTableAlias = localTableAlias;
	}

    /**
     * Return the joined table name.
     */
    public String getTable() {
        return table;
    }

    /**
     * set the joined table name.
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Return the type of join. LEFT OUTER JOIN etc.
     */
    public String getType() {
        return type;
    }

    /**
     * Return true if this join is a left outer join.
     */
    public boolean isOuterJoin() {
        return type.equals(TableJoin.LEFT_OUTER);
    }
    
    /**
     * Set the type of join.
     */
    public void setType(String joinType) {
        joinType = joinType.toUpperCase();
        if (joinType.equalsIgnoreCase(TableJoin.JOIN)) {
            type = TableJoin.JOIN;
        } else if (joinType.indexOf("LEFT") > -1) {
            type = TableJoin.LEFT_OUTER;
        } else if (joinType.indexOf("OUTER") > -1) {
            type = TableJoin.LEFT_OUTER;
        } else if (joinType.indexOf("INNER") > -1) {
            type = TableJoin.JOIN;
        } else {
            throw new RuntimeException(Message.msg("join.type.unknown", joinType));
        }
    }
    
    public DeployTableJoin createInverse() {
    	
    	DeployTableJoin inverse = new DeployTableJoin();
    	inverse.setTable("ERROR:CHANGE THE TABLE");
    	inverse.setForeignTableAlias(localTableAlias);
    	inverse.setLocalTableAlias(foreignTableAlias);
    	inverse.setType(type);
    	
    	DeployTableJoinColumn[] cols = columns();
    	for (int i = 0; i < cols.length; i++) {
    		inverse.addJoinColumn(cols[i].createInverse());
		}
    	
    	return inverse;
    }
}
