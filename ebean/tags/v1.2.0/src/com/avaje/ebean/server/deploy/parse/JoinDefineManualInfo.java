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
package com.avaje.ebean.server.deploy.parse;

import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.JoinTable;

import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;

/**
 * Used to help finish defining manually describes joins.
 * <p>
 * Specifically find the other side of the join if it is not specified and
 * determine if the join should be an outer join based on if the column is
 * nullable.
 * </p>
 */
public class JoinDefineManualInfo {

	final DeployBeanDescriptor<?> descriptor;

	final DeployBeanPropertyAssoc<?> property;

	final DeployTableJoin tableJoin;

	/**
	 * Used for a Secondary table.
	 */
	public JoinDefineManualInfo(DeployBeanDescriptor<?> desc, DeployTableJoin join) {
		this.descriptor = desc;
		this.tableJoin = join;
		this.property = null;

	}

	public JoinDefineManualInfo(DeployBeanDescriptor<?> desc, DeployBeanPropertyAssoc<?> prop) {
		this.descriptor = desc;
		this.property = prop;
		this.tableJoin = (prop == null) ? null : prop.getTableJoin();
	}

	public String getDebugName() {
		String s = descriptor.getFullName();
		if (property != null) {
			s += "." + property.getName();
		}
		return s;
	}

	/**
	 * Return the associated bean property.
	 */
	public DeployBeanPropertyAssoc<?> getProperty() {
		return property;
	}

	/**
	 * Return the associated bean descriptor.
	 */
	public DeployBeanDescriptor<?> getDescriptor() {
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
	public void add(boolean order, JoinTable joinTable, JoinColumn[] cols) {

		if (!"".equals(joinTable.name())) {
			this.tableJoin.setTable(joinTable.name());
		}

		tableJoin.addJoinColumn(order, cols);
	}

	/**
	 * Add JoinColumns information.
	 */
	public void add(boolean order, JoinColumns joinColumns) {
		tableJoin.addJoinColumn(order, joinColumns.value());
	}

	/**
	 * Add JoinColumn information.
	 * <p>
	 * The order is generally true for OneToMany and false for ManyToOne.
	 * </p>
	 */
	public void add(boolean order, JoinColumn joinColumn) {
		tableJoin.addJoinColumn(order, joinColumn);
	}

}
