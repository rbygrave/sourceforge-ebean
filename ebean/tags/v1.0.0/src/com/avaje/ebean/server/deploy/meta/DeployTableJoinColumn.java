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

/**
 * A join pair of local and foreign properties.
 */
public class DeployTableJoinColumn {

	/**
	 * The local database column name.
	 */
	String localDbColumn;

	/**
	 * The foreign database column name.
	 */
	String foreignDbColumn;

	// /**
	// * Create the pair.
	// */
	// public DeployTableJoinColumn() {
	//
	// }

	public DeployTableJoinColumn(String localDbColumn, String foreignDbColumn) {
		this.localDbColumn = localDbColumn;
		this.foreignDbColumn = foreignDbColumn;
	}

	/**
	 * Create a TableJoinColumn with the local and foreign columns swapped.
	 */
	public DeployTableJoinColumn createInverse() {
		return new DeployTableJoinColumn(foreignDbColumn, localDbColumn);
	}

	public String toString() {
		return localDbColumn + " = " + foreignDbColumn;
	}

	/**
	 * Return true if either the local or foreign column is null.
	 * <p>
	 * Both columns need to be defined. If one is null then typically it is
	 * derived as the primary key column.
	 * </p>
	 */
	public boolean hasNullColumn() {
		return localDbColumn == null || foreignDbColumn == null;
	}

	/**
	 * When only ONE column has been set by deployment information return that one.
	 * <p>
	 * Used with hasNullColumn() to set the foreignDbColumn for OneToMany joins.
	 * </p>
	 */
	public String getNonNullColumn() {
		if (localDbColumn == null && foreignDbColumn == null) {
			throw new IllegalStateException("expecting only one null column?");
			
		} else if (localDbColumn != null && foreignDbColumn != null) {
			throw new IllegalStateException("expecting one null column?");			
		}
		if (localDbColumn != null) {
			return localDbColumn;
		} else {			
			return foreignDbColumn;
		}
	}

	/**
	 * Return the foreign database column name.
	 */
	public String getForeignDbColumn() {
		return foreignDbColumn;
	}

	/**
	 * Set the foreign database column name.
	 * <p>
	 * Used when this is derived from Primary Key and not set explicitly in the
	 * deployment information.
	 * </p>
	 */
	public void setForeignDbColumn(String foreignDbColumn) {
		this.foreignDbColumn = foreignDbColumn;
	}

	/**
	 * Return the local database column name.
	 */
	public String getLocalDbColumn() {
		return localDbColumn;
	}

	/**
	 * Set the local database column name.
	 */
	public void setLocalDbColumn(String localDbColumn) {
		this.localDbColumn = localDbColumn;
	}

}
