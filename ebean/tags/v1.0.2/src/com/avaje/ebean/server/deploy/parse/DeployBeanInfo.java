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

import java.util.HashMap;

import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;

/**
 * Wraps information about a bean during deployment parsing.
 * <p>
 * This is passed around while all the deployment information is being collected
 * from annotations, xml and database meta data (foreign keys etc).
 * </p>
 */
public class DeployBeanInfo {
	
	TableAliasList aliasList = new TableAliasList();

	/**
	 * Holds TableJoins. These can be created in annotations but overridden in
	 * deployment xml file.
	 */
	HashMap<String,DeployTableJoin> tableJoinMap = new HashMap<String, DeployTableJoin>();

	DeployUtil util;

	DeployBeanDescriptor descriptor;

	/**
	 * Create with a DeployUtil and BeanDescriptor.
	 */
	public DeployBeanInfo(DeployUtil util, DeployBeanDescriptor descriptor) {
		this.util = util;
		this.descriptor = descriptor;
	}

	public String toString() {
		return ""+descriptor;
	}
	
	/**
	 * Return the BeanDescriptor currently being processed.
	 */
	public DeployBeanDescriptor getDescriptor() {
		return descriptor;
	}

	/**
	 * Return the DeployUtil we are using.
	 */
	public DeployUtil getUtil() {
		return util;
	}

	private String tableNameFromClass(Class<?> beanType) {
		return util.getTableNameFromClass(beanType);
	}

	/**
	 * Set the default table name if it has not already been set.
	 * <p>
	 * This will use the NamingConvention but JPA spec defaults this to the
	 * class name.
	 * </p>
	 */
	public void setDefaultTableName() {

		if (!descriptor.isEmbedded()) {
			String baseTable = descriptor.getBaseTable();
			if (baseTable == null) {
				// default the tableName using NamingConvention.
				// JPA Spec defines this as the class name
				String tableName = tableNameFromClass(descriptor.getBeanType());
				setTable("", "", tableName, null);
			}
		}
	}

	/**
	 * Appropriate TableJoin for a property mapped to a secondary table.
	 */
	public DeployTableJoin getTableJoin(String tableName, String alias) {

		// this is ok even if alias is null which is normal
		String key = tableName.toLowerCase() + "." + alias;

		DeployTableJoin tableJoin = (DeployTableJoin) tableJoinMap.get(key);
		if (tableJoin == null) {
			// make sure we get a unique alias to use
			alias = getAlias(tableName, alias);

			tableJoin = new DeployTableJoin();
			tableJoin.setLocalTableAlias(descriptor.getBaseTableAlias());
			tableJoin.setForeignTableAlias(alias);
			tableJoin.setTable(tableName);
			tableJoin.setType(TableJoin.JOIN);
			descriptor.addTableJoin(tableJoin);

			tableJoinMap.put(key, tableJoin);
		}
		return tableJoin;
	}

	/**
	 * Set a the join alias for a assoc one property.
	 */
	public void setBeanJoinAlias(DeployBeanPropertyAssocOne beanProp, boolean annOptional) {

		String joinType = TableJoin.JOIN;
		if (annOptional && util.isUseOneToOneOptional()) {
			joinType = TableJoin.LEFT_OUTER;
		}

		//FIXME: check tableAlias is not a keyword
		// get a unique alias possibly using the name of the property
		String alias = getAlias(beanProp.getName(), null);

		DeployTableJoin tableJoin = beanProp.getTableJoin();
		tableJoin.setType(joinType);
		tableJoin.setForeignTableAlias(alias);
		tableJoin.setLocalTableAlias(descriptor.getBaseTableAlias());
	}

	/**
	 * Set a the join alias for a assoc many property.
	 */
	public void setManyJoinAlias(DeployBeanPropertyAssocMany listProp, DeployTableJoin tableJoin) {

		// get a unique alias possibly using the name of the property
		String alias = getAlias(listProp.getName(), null);

		// its always going to be an outer join... as perhaps no rows
		tableJoin.setType(TableJoin.LEFT_OUTER);
		tableJoin.setForeignTableAlias(alias);
		tableJoin.setLocalTableAlias(descriptor.getBaseTableAlias());
	}

	/**
	 * ManyToMany only, create an alias for the source to intersection table
	 * join.
	 */
	public void setManyIntersectionAlias(DeployBeanPropertyAssocMany listProp,
			DeployTableJoin tableJoin) {

		// get a unique alias possibly using the name of the property
		String alias = getAlias(listProp.getName(), null);

		// its always going to be an outer join... as perhaps no rows
		tableJoin.setType(TableJoin.LEFT_OUTER);
		tableJoin.setForeignTableAlias(alias);
		tableJoin.setLocalTableAlias(descriptor.getBaseTableAlias());
	}

	/**
	 * Set a the join alias for a TableJoin (Secondary table).
	 */
	public void setTableJoinAlias(DeployTableJoin tableJoin, String type) {
		String alias = getAlias(tableJoin.getTable(), null);
		tableJoin.setType(type);
		tableJoin.setForeignTableAlias(alias);
		tableJoin.setLocalTableAlias(descriptor.getBaseTableAlias());
	}

	/**
	 * Set the base table name and alias.
	 */
	public void setTable(String catalog, String schema, String tableName, String alias) {
		
		if (tableName != null && tableName.trim().length() > 0) {

			alias = getAlias(tableName, alias);

			tableName = util.convertQuotedIdentifiers(tableName);

			if (schema != null && schema.length() > 0){
				tableName = schema+"."+tableName;
			}
			if (catalog != null && catalog.length() > 0){
				tableName = catalog+"."+tableName;
			}
			
			descriptor.setBaseTable(tableName);
			descriptor.setBaseTableAlias(alias);
		}
	}

	private String getAlias(String tableOrProperty, String defaultValue) {

		String alias = getAliasAttempt(tableOrProperty, defaultValue);
		if (!isKeyword(alias)){
			return alias;
		}
		for (int i = 0; i < 10; i++) {
			// try to get a alias that is not a keyword
			alias = getAliasAttempt(tableOrProperty, null);
			if (!isKeyword(alias)){
				return alias;
			}
		}
		return "_z_";
	}
	
	/**
	 * Get a table alias for TableJoin and associated Bean joins.
	 */
	private String getAliasAttempt(String tableOrProperty, String defaultValue) {

		if (defaultValue != null && defaultValue.length() > 1) {
			// if its more that one character it is definitely used...
			return defaultValue;
		}
		if (defaultValue != null && aliasList.remove(defaultValue)) {
			return defaultValue;
		}

		String alias = util.getPotentialAlias(tableOrProperty);

		if (!aliasList.remove(alias)) {
			// just remove the next one
			alias = aliasList.removeNext();
		}
		return alias;
	}
	
	private boolean isKeyword(String tableAlias){
		return SqlReservedWords.isKeyword(tableAlias);
	}

}
