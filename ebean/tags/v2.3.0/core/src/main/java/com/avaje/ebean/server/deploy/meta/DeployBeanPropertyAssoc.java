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

import com.avaje.ebean.server.deploy.BeanTable;
import com.avaje.ebean.server.deploy.BeanCascadeInfo;

/**
 * Abstract base for properties mapped to an associated bean, list, set or map.
 */
public abstract class DeployBeanPropertyAssoc<T> extends DeployBeanProperty {

	/**
	 * The type of the joined bean.
	 */
	Class<T> targetType;

	/**
	 * Persist settings.
	 */
	BeanCascadeInfo cascadeInfo = new BeanCascadeInfo();

	/**
	 * The join table information.
	 */
	BeanTable beanTable;

	/**
	 * Join between the beans.
	 */
	DeployTableJoin tableJoin = new DeployTableJoin();

	/**
	 * Whether the associated join type should be an outer join.
	 */
	boolean isOuterJoin = false;

	/**
	 * Literal added to where clause of lazy loading query.
	 */
	String extraWhere;

	/**
	 * From the deployment mappedBy attribute.
	 */
	String mappedBy;
	
	/**
	 * Construct the property.
	 */
	public DeployBeanPropertyAssoc(DeployBeanDescriptor<?> desc, Class<T> targetType) {
		super(desc, targetType, null);
		this.targetType = targetType;
	}

	/**
	 * Return false.
	 */
	@Override
	public boolean isScalar() {
		return false;
	}

	/**
	 * Return the type of the target.
	 * <p>
	 * This is the class of the associated bean, or beans contained in a list,
	 * set or map.
	 * </p>
	 */
	public Class<T> getTargetType() {
		return targetType;
	}

//	/**
//	 * Set the class of the target.
//	 */
//	public void setTargetType(Class<?> targetType) {
//		this.targetType = targetType;
//	}

	/**
	 * Return if this association should use an Outer join.
	 */
	public boolean isOuterJoin() {
		return isOuterJoin;
	}

	/**
	 * Specify that this bean should use an outer join.
	 */
	public void setOuterJoin(boolean isOuterJoin) {
		this.isOuterJoin = isOuterJoin;
	}

	/**
	 * Return a literal expression that is added to the query that lazy loads
	 * the collection.
	 */
	public String getExtraWhere() {
		return extraWhere;
	}

	/**
	 * Set a literal expression to add to the query that lazy loads the
	 * collection.
	 */
	public void setExtraWhere(String extraWhere) {
		this.extraWhere = extraWhere;
	}
	
	/**
	 * return the join to use for the bean.
	 */
	public DeployTableJoin getTableJoin() {
		return tableJoin;
	}

	/**
	 * Return the BeanTable for this association.
	 * <p>
	 * This has the table name which is used to determine the relationship for
	 * this association.
	 * </p>
	 */
	public BeanTable getBeanTable() {
		return beanTable;
	}

	/**
	 * Set the bean table.
	 */
	public void setBeanTable(BeanTable beanTable) {
		this.beanTable = beanTable;
		getTableJoin().setTable(beanTable.getBaseTable());
	}

	/**
	 * Get the persist info.
	 */
	public BeanCascadeInfo getCascadeInfo() {
		return cascadeInfo;
	}


	/**
	 * Return the mappedBy deployment attribute.
	 * <p>
	 * This is the name of the property in the 'detail' bean that maps back to
	 * this 'master' bean.
	 * </p>
	 */
	public String getMappedBy() {
		return mappedBy;
	}

	/**
	 * Set mappedBy deployment attribute.
	 */
	public void setMappedBy(String mappedBy) {
		if (!"".equals(mappedBy)) {
			this.mappedBy = mappedBy;
		}
	}
}
