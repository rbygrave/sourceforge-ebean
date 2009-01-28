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
package com.avaje.ebean.server.deploy.jointree;

/**
 * Holds logical and physical deployment names for a property.
 * <p>
 * This is for a given BeanJoinTree and includes the table and property prefixes
 * for this node.
 * </p>
 */
public class PropertyDeploy {

	/**
	 * The full logical property name.
	 */
	private final String logical;

	/**
	 * The full database table alias and column name.
	 */
	private final String deploy;

	/**
	 * Flag set when its a foreign key. 
	 * Used to convert the deploy and include if possible so as to reduce 
	 * extra joins.
	 */
	private final boolean foreignKey;
	
	/**
	 * Typically the same as the logical property name, but different for
	 * foreign keys. This is used so as to reduce the need for an extraneous
	 * join for foreign key columns.
	 */
	private final String include;

	/**
	 * Initial normal constructor.
	 * @param foreignKey
	 * @param logical
	 * @param deploy
	 */
	public PropertyDeploy(boolean foreignKey, String logical, String deploy) {
		this(foreignKey, logical, deploy, logical);
	}
	
	/**
	 * For use when converting foreign key column.
	 */
	public PropertyDeploy(boolean foreignKey, String logical, String deploy, String include) {
		this.foreignKey = foreignKey;
		this.logical = logical;
		this.deploy = deploy;
		this.include = include;
	}

	/**
	 * Create a matching import FK property based on this one.
	 */
	public PropertyDeploy createFkey(String logicalImportedId) {
		return new PropertyDeploy(true, logicalImportedId, deploy, include);
	}
	
	/**
	 * Return true if this is a foreign key.
	 * If so then it does not require a join from its parent
	 * unlike other properties.
	 */
	public boolean isForeignKey() {
		return foreignKey;
	}

	
	/**
	 * Return the include that should be used for property if it is a foreignKey.
	 */
	public String getInclude() {
		return include;
	}

	/**
	 * Return the deployment value.
	 */
	public String getDeploy() {
		return deploy;
	}

	/**
	 * Return the logical value.
	 */
	public String getLogical() {
		return logical;
	}

	/**
	 * A description of the values.
	 */
	public String toString() {
		return logical+"="+deploy;
	}
}
