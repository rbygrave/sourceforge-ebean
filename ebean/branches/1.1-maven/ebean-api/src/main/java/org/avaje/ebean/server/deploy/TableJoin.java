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
package org.avaje.ebean.server.deploy;

import java.sql.SQLException;
import java.util.LinkedHashMap;

import org.avaje.ebean.server.deploy.jointree.JoinNode;
import org.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import org.avaje.ebean.server.deploy.meta.DeployTableJoin;
import org.avaje.ebean.server.deploy.meta.DeployTableJoinColumn;

/**
 * Represents a join to another table.
 */
public final class TableJoin {

	
    public static final String NEW_LINE = "\n";
    
    public static final String LEFT_OUTER = "left outer join";

    public static final String JOIN = "join";
    
    /**
     * Flag set when the imported key maps to the primary key.
     * This occurs for intersection tables (ManyToMany).
     */
    private final boolean importedPrimaryKey;
    
    /**
     * The joined table.
     */
    private final String table;

    /**
     * The table alias for the joined table.
     */
    private final String foreignTableAlias;

    /**
     * The table alias for local base table.
     */
    private final String localTableAlias;
    
    /**
     * The type of join. LEFT OUTER etc.
     */
    private final String type;

    /**
     * The persist cascade info.
     */
    private final BeanCascadeInfo cascadeInfo;
    
    /**
     * Properties as an array.
     */
    private final BeanProperty[] properties;
    
    /**
     * Columns as an array.
     */
    private final TableJoinColumn[] columns;

    /**
     * Create a TableJoin.
     */
    public TableJoin(DeployTableJoin deploy, LinkedHashMap<String,BeanProperty> propMap) {
    	
        this.importedPrimaryKey = deploy.isImportedPrimaryKey();
        this.table = deploy.getTable();
        this.foreignTableAlias = deploy.getForeignTableAlias();
        this.localTableAlias = deploy.getLocalTableAlias();
        this.type = deploy.getType();
        this.cascadeInfo = deploy.getCascadeInfo();
        
        DeployTableJoinColumn[] deployCols = deploy.columns();
        this.columns = new TableJoinColumn[deployCols.length];
        for (int i = 0; i < deployCols.length; i++) {
			this.columns[i] = new TableJoinColumn(deployCols[i]);
		}
        
        DeployBeanProperty[] deployProps = deploy.properties();
        if (deployProps.length > 0 && propMap == null){
        	throw new NullPointerException("propMap is null?");
        }
        
        this.properties = new BeanProperty[deployProps.length];
        for (int i = 0; i < deployProps.length; i++) {
        	BeanProperty prop = propMap.get(deployProps[i].getName());
        	this.properties[i] = prop;
		}
        
    }

    /**
     * Create a tableJoin based on this object but with different alias.
     */
	public TableJoin createWithAlias(String localAlias, String foreignAlias) {
    
		return new TableJoin(this, localAlias, foreignAlias);
	}
	
	/**
	 * Construct a copy but with different table alias'.
	 */
	private TableJoin(TableJoin join, String localAlias, String foreignAlias){

		this.foreignTableAlias = foreignAlias;
		this.localTableAlias = localAlias;

		// copy the immutable fields
		this.importedPrimaryKey = join.importedPrimaryKey;
		this.table = join.table;
		this.type = join.type;
		this.cascadeInfo = join.cascadeInfo;
		this.properties = join.properties;
		this.columns = join.columns;
	}

		
    public String toString() {
        String s =  type + " " + table + " ";
        for (int i = 0; i < columns.length; i++) {
			s += columns[i]+" ";
		}
        return s;
    }

    public void appendSelect(DbSqlContext ctx) {
    	for (int i = 0, x = properties.length; i < x; i++) {
    		properties[i].appendSelect(ctx);
		}
    }
    
    public Object readSet(DbReadContext ctx, Object bean, Class<?> type) throws SQLException {
    	for (int i = 0, x = properties.length; i < x; i++) {
    		properties[i].readSet(ctx, bean, type);
		}
    	return null;
    }
    
    /**
     * Return true if the imported foreign key maps to the primary key.
     */
    public boolean isImportedPrimaryKey() {
		return importedPrimaryKey;
	}

    /**
     * Return the persist info.
     */
    public BeanCascadeInfo getCascadeInfo() {
        return cascadeInfo;
    }

    /**
     * Return the join columns.
     */
    public TableJoinColumn[] columns() {
    	return columns;
    }

    
    /**
     * For secondary table joins returns the properties mapped to that table.
     */
    public BeanProperty[] properties() {
    	return properties;
    }

    /**
     * Return the table alias used by this join.
     */
    public String getForeignTableAlias() {
        return foreignTableAlias;
    }

    /**
     * Return the local base table alias.
     */
    public String getLocalTableAlias() {
		return localTableAlias;
	}

    /**
     * Return the joined table name.
     */
    public String getTable() {
        return table;
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
        return type.equals(LEFT_OUTER);
    }
    
	/**
     * Generate the on clause.
     */
    private void addOnClause(String ZZprefix, DbSqlContext ctx) {
        
    	ctx.append(" on ");
    	
        TableJoinColumn[] cols = columns();
        for (int i = 0; i < cols.length; i++) {
            TableJoinColumn pair = cols[i];
            if (i > 0) {
            	ctx.append(" and ");
            }
            
            ctx.append(localTableAlias);
            ctx.append(".").append(pair.getLocalDbColumn());
            ctx.append(" = ");
            ctx.append(foreignTableAlias);
            ctx.append(".").append(pair.getForeignDbColumn());
        }
        
        ctx.append(" ");
    }

    public void addJoin(boolean forceOuterJoin, JoinNode node, DbSqlContext ctx) {
    	
    	
    	ctx.append(NEW_LINE);
    	if (forceOuterJoin){
    		ctx.append(LEFT_OUTER);
    	} else {
    		ctx.append(type);
    	}
    	ctx.append(" ").append(table).append(" ");
    	ctx.append(foreignTableAlias);
    	addOnClause(null, ctx);
    }
}
