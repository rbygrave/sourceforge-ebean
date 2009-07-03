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

import com.avaje.ebean.config.naming.NamingConvention;
import com.avaje.ebean.config.naming.TableName;
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
public class DeployBeanInfo<T> {

	/**
	 * Holds TableJoins. These can be created in annotations but overridden in
	 * deployment xml file.
	 */
	private final HashMap<String,DeployTableJoin> tableJoinMap = new HashMap<String, DeployTableJoin>();

	private final DeployUtil util;

	private final DeployBeanDescriptor<T> descriptor;

	/**
	 * Create with a DeployUtil and BeanDescriptor.
	 */
	public DeployBeanInfo(DeployUtil util, DeployBeanDescriptor<T> descriptor) {
		this.util = util;
		this.descriptor = descriptor;
	}

	public String toString() {
		return ""+descriptor;
	}

	/**
	 * Return the BeanDescriptor currently being processed.
	 */
	public DeployBeanDescriptor<T> getDescriptor() {
		return descriptor;
	}

	/**
	 * Return the DeployUtil we are using.
	 */
	public DeployUtil getUtil() {
		return util;
	}

	/**
	 * Set the table name if it has not already been set.
	 * <p>
	 * This will use the NamingConvention as provided in the ServerConfig
	 * The default is to take the class name as defined by JPA spec
	 * </p>
	 * @see NamingConvention
	 */
	public void setTableName() {

		if (!descriptor.isEmbedded() && !descriptor.isMeta()) {

			// default the TableName using NamingConvention.
			TableName tableName = util.getTableNameFromClass(descriptor.getBeanType());
			
            descriptor.setBaseTable(tableName.getQualifiedName());
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
			tableJoin = new DeployTableJoin();
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
	public void setBeanJoinAlias(DeployBeanPropertyAssocOne<?> beanProp, boolean outerJoin) {

		String joinType = TableJoin.JOIN;
		if (outerJoin){// && util.isUseOneToOneOptional()) {
			joinType = TableJoin.LEFT_OUTER;
		}

		DeployTableJoin tableJoin = beanProp.getTableJoin();
		tableJoin.setType(joinType);
	}

	/**
	 * Set a the join alias for a assoc many property.
	 */
	public void setManyJoinAlias(DeployBeanPropertyAssocMany<?> listProp, DeployTableJoin tableJoin) {

		// its always going to be an outer join... as perhaps no rows
		tableJoin.setType(TableJoin.LEFT_OUTER);
	}

	/**
	 * ManyToMany only, create an alias for the source to intersection table
	 * join.
	 */
	public void setManyIntersectionAlias(DeployBeanPropertyAssocMany<?> listProp,
			DeployTableJoin tableJoin) {

		// its always going to be an outer join... as perhaps no rows
		tableJoin.setType(TableJoin.LEFT_OUTER);
	}

}
