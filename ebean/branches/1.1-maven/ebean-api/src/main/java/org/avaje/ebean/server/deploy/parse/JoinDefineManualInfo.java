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
package org.avaje.ebean.server.deploy.parse;

import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;

import org.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import org.avaje.ebean.server.deploy.meta.DeployTableJoin;
import org.avaje.ebean.server.deploy.meta.DeployTableJoinColumn;
import org.avaje.lib.util.StringHelper;;

public class JoinDefineManualInfo {

	DeployBeanDescriptor descriptor;

	DeployBeanPropertyAssoc property;

	DeployTableJoin tableJoin;
	
	/**
	 * Used for a Secondary table.
	 */
	public JoinDefineManualInfo(DeployBeanDescriptor desc, DeployTableJoin join) {
		this(desc, null, true);
		this.tableJoin = join;

	}

	/**
	 * Used for joins on ManyToOne and OneToOne.
	 */
	public JoinDefineManualInfo(DeployBeanDescriptor desc, DeployBeanPropertyAssocOne prop) {
		this(desc, prop, true);
	}

	/**
	 * Used for joins on OneToMany and ManyToMany.
	 */
	public JoinDefineManualInfo(DeployBeanDescriptor desc, DeployBeanPropertyAssocMany prop) {
		this(desc, prop, false);
	}

	private JoinDefineManualInfo(DeployBeanDescriptor desc, DeployBeanPropertyAssoc prop, boolean isImported) {
		this.descriptor = desc;
		this.property = prop;
		if (prop != null) {
			this.tableJoin = prop.getTableJoin();
		}
	}

	public String getDebugName() {
		String s = descriptor.getFullName();
		if (property != null){
			s += "."+property.getName();
		}
		return s;
	}

	/**
	 * Return the associated bean property.
	 */
	public DeployBeanPropertyAssoc getProperty() {
		return property;
	}

	/**
	 * Return the associated bean descriptor.
	 */
	public DeployBeanDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Return the associated TableJoin.
	 */
	public DeployTableJoin getTableJoin() {
		return tableJoin;
	}
	
	/**
	 * Add deployment info from JoinTable annotation.
	 */
	public void add(JoinTable joinTable){
		
		String table = joinTable.name();
		if (!StringHelper.isNull(table)){
			this.tableJoin.setTable(joinTable.name());			
		}
		//joinTable.schema();
		//joinTable.catalog();
		
		JoinColumn[] cols = joinTable.joinColumns();
    	    
	    for (int i = 0; i < cols.length; i++) {
	    	add(cols[i]);
		}
	}
	
	/**
	 * Add deployment info from JoinColumns annotation.
	 */	
	public void add(JoinColumns joinColumns){
		JoinColumn[] cols = joinColumns.value();
		for (int i = 0; i < cols.length; i++) {
			add(cols[i]);
		}
	}
	
	/**
	 * Add deployment info from JoinColumn annotation.
	 */
	public void add(JoinColumn joinColumn){
		
		if (!"".equals(joinColumn.table())) {
			tableJoin.setTable(joinColumn.table());
		}
		
		// not using this for now...
		//joinColumn.columnDefinition();
		//joinColumn.insertable();
		//joinColumn.unique();
		//joinColumn.updatable();
		
		
		//TODO: Review nullable which controls OUTER JOIN vs INNER JOIN
		// a boolean is never optional, so never sure when Ebean
		// should use DB meta data to check nullable
		//joinColumn.nullable();
		
		String localColumn = nullEmptyString(joinColumn.name());
		String refColumn = nullEmptyString(joinColumn.referencedColumnName());
		
		add(new DeployTableJoinColumn(localColumn, refColumn));
	}
	
	private String nullEmptyString(String s){
		if ("".equals(s)){
			return null;
		}
		return s;
	}
	
	private void add(DeployTableJoinColumn column){
		tableJoin.addTableJoinColumn(column);
	}
}
