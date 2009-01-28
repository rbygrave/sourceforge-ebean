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

import java.util.HashSet;
import java.util.List;

import org.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssoc;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import org.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import org.avaje.ebean.server.deploy.meta.DeployTableJoin;
import org.avaje.ebean.server.lib.sql.Fkey;
import org.avaje.ebean.server.lib.sql.FkeyColumn;
import org.avaje.ebean.server.naming.NamingConvention;

/**
 * Parameters used to define joins.
 * <p>
 * This is used to determine joins from deployment information such as
 * JoinColumns, JoinColumn and JoinTable annotations and also to define joins
 * when no such deployment information exists.
 * </p>
 */
public class JoinDefineAutomaticInfo {

	HashSet<String> matchColumns = new HashSet<String>();

	DeployBeanDescriptor desc;

	DeployBeanPropertyAssoc prop;

	DeployTableJoin tableJoin;

	String joinTableName;

	boolean isImported;

	boolean oneToOne;

	/**
	 * Used for Secondary table joins.
	 */
	public JoinDefineAutomaticInfo(DeployBeanDescriptor desc, DeployTableJoin join) {
		this(desc, null, true);
		this.tableJoin = join;
	}

	/**
	 * Used for joins on ManyToOne and OneToOne.
	 */
	public JoinDefineAutomaticInfo(DeployBeanDescriptor desc, DeployBeanPropertyAssocOne prop) {
		this(desc, prop, true);
		this.oneToOne = prop.isOneToOne();
	}

	/**
	 * Used for joins on oneToMany and ManyToMany.
	 */
	public JoinDefineAutomaticInfo(DeployBeanDescriptor desc, DeployBeanPropertyAssocMany prop) {
		this(desc, prop, false);
	}

	private JoinDefineAutomaticInfo(DeployBeanDescriptor desc, DeployBeanPropertyAssoc prop, boolean isImported) {
		this.desc = desc;
		this.prop = prop;
		this.isImported = isImported;
		if (prop != null) {
			this.tableJoin = prop.getTableJoin();
		}
	}

	/**
	 * We have found the matching foreign key. Set the details.
	 */
	public void setMatch(Fkey fkey) {
		tableJoin.addColumns(fkey);
		if (tableJoin.isOuterJoin() && prop != null) {
			prop.setOuterJoin(true);
		}
	}

	/**
	 * Test to see if this Foreign key is a match.
	 */
	private boolean isMatch(Fkey fkey) {
		FkeyColumn[] cols = fkey.columns();
		if (matchColumns.size() != cols.length) {
			return false;
		}
		for (int i = 0; i < cols.length; i++) {
			String colName = cols[i].getFkColumnName().toLowerCase();
			if (!matchColumns.contains(colName)) {
				return false;
			}
		}

		return true;
	}

	/**
	 * If no explicit join columns have been set for matching against, create a
	 * guess based on the property name. This is used when multiple Fkeys exist
	 * between the two respective tables.
	 * <p>
	 * Example Customer has a Billing Address and Postal Address. Guess
	 * postal_address_id based on property name postalAddress.
	 * </p>
	 */
	private void addGuess(NamingConvention namingConvention) {

		String fkGuess = namingConvention.getForeignKeyColumn(prop.getName());
		addColumn(fkGuess);
	}

	/**
	 * Find a match from the list of foreign keys.
	 * <p>
	 * The namingConvention is used to guess a foreign key name based on the
	 * property name if no join columns have been defined.
	 * </p>
	 */
	public boolean findMatch(List<Fkey> fkeyList, NamingConvention namingConvention) {

		if (matchColumns.isEmpty() && prop != null) {
			addGuess(namingConvention);
		}

		for (int i = 0; i < fkeyList.size(); i++) {
			Fkey fkey = (Fkey) fkeyList.get(i);
			if (isMatch(fkey)) {
				setMatch(fkey);
				return true;
			}
		}

		return false;
	}

	/**
	 * Set the join Table name.
	 */
	public void setJoinTableName(String joinTableName) {
		this.joinTableName = joinTableName;
	}

	/**
	 * Add a column to match against.
	 */
	public void addColumn(String column) {
		matchColumns.add(column.toLowerCase());
	}

	/**
	 * Return true if we are looking for imported fkeys.
	 */
	public boolean isImported() {
		return isImported;
	}

	/**
	 * return true if this property is a OneToOne relationship.
	 * <p>
	 * We need to look in both imported and exported fkeys.
	 * </p>
	 */
	public boolean isOneToOne() {
		return oneToOne;
	}

	/**
	 * This is on a OneToOne property.
	 * <p>
	 * This means we don't know if we are checking against imported or exported
	 * fkeys so we need to check both.
	 * </p>
	 */
	public void setOneToOne(boolean oneToOne) {
		this.oneToOne = oneToOne;
	}

	/**
	 * Return the BeanDescriptor.
	 */
	public DeployBeanDescriptor getDesc() {
		return desc;
	}

	/**
	 * Return the join table name.
	 */
	public String getJoinTableName() {
		return joinTableName;
	}

	/**
	 * Return the property we are setting the join for.
	 */
	public DeployBeanPropertyAssoc getProp() {
		return prop;
	}

	/**
	 * Return the TableJoin we are setting.
	 */
	public DeployTableJoin getTableJoin() {
		return tableJoin;
	}

	/**
	 * Return an array for an error message.
	 */
	public String[] getErrorArray() {
		String[] errors = new String[3];
		errors[0] = desc.getFullName();
		errors[1] = tableJoin.getTable();
		if (prop != null) {
			errors[2] = prop.getName();
		}

		return errors;
	}

}
