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

import com.avaje.ebean.Query;
import com.avaje.ebean.server.deploy.TableJoin;

/**
 * Property mapped to a List Set or Map.
 */
public class DeployBeanPropertyAssocMany<T> extends DeployBeanPropertyAssoc<T> {

	/**
	 * Flag to indicate manyToMany relationship.
	 */
	boolean manyToMany;

	/**
	 * Flag to indicate this is a unidirectional relationship.
	 */
	boolean unidirectional;

	/**
	 * Join for manyToMany intersection table.
	 */
	DeployTableJoin intersectionJoin;

	/**
	 * For ManyToMany this is the Inverse join used to build reference queries.
	 */
	DeployTableJoin inverseJoin;

	String fetchOrderBy;

	String mapKey;

	/**
	 * The type of the many, set, list or map.
	 */
	Query.Type manyType;

	/**
	 * Create this property.
	 */
	public DeployBeanPropertyAssocMany(DeployBeanDescriptor<?> desc, Class<T> targetType, Query.Type manyType) {
		super(desc, targetType);
		this.manyType = manyType;
	}

	/**
	 * When generics is not used for manyType you can specify via annotations.
	 * <p>
	 * Really only expect this for Scala due to a Scala compiler bug at the moment.
	 * Otherwise I'd probably not bother support this.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	public void setTargetType(Class<?> cls){
		this.targetType = (Class<T>)cls;
	}
	
	/**
	 * Return the many type.
	 */
	public Query.Type getManyType() {
		return manyType;
	}

	/**
	 * Return true if this is many to many.
	 */
	public boolean isManyToMany() {
		return manyToMany;
	}

	/**
	 * Set to true if this is a many to many.
	 */
	public void setManyToMany(boolean isManyToMany) {
		this.manyToMany = isManyToMany;
	}

	/**
	 * Return true if this is a unidirectional relationship.
	 */
	public boolean isUnidirectional() {
		return unidirectional;
	}

	/**
	 * Set to true if this is a unidirectional relationship.
	 */
	public void setUnidirectional(boolean unidirectional) {
		this.unidirectional = unidirectional;
	}

	/**
	 * Create the immutable version of the intersection join.
	 */
	public TableJoin createIntersectionTableJoin() {
		if (intersectionJoin != null){
			return new TableJoin(intersectionJoin, null);
		} else {
			return null;
		}
	}
	
	/**
	 * Create the immutable version of the inverse join.
	 */
	public TableJoin createInverseTableJoin() {
		if (inverseJoin != null){
			return new TableJoin(inverseJoin, null);
		} else {
			return null;
		}
	}
	
	/**
	 * ManyToMany only, join from local table to intersection table.
	 */
	public DeployTableJoin getIntersectionJoin() {
		return intersectionJoin;
	}

	public DeployTableJoin getInverseJoin() {
		return inverseJoin;
	}

	/**
	 * ManyToMany only, join from local table to intersection table.
	 */
	public void setIntersectionJoin(DeployTableJoin intersectionJoin) {
		this.intersectionJoin = intersectionJoin;
	}

	/**
	 * ManyToMany only, join from foreign table to intersection table.
	 */
	public void setInverseJoin(DeployTableJoin inverseJoin) {
		this.inverseJoin = inverseJoin;
	}

	/**
	 * Return the order by clause used to order the fetching of the data for
	 * this list, set or map.
	 */
	public String getFetchOrderBy() {
		return fetchOrderBy;
	}

	/**
	 * Return the default mapKey when returning a Map.
	 */
	public String getMapKey() {
		return mapKey;
	}

	/**
	 * Set the default mapKey to use when returning a Map.
	 */
	public void setMapKey(String mapKey) {
		if (mapKey != null && mapKey.length() > 0) {
			this.mapKey = mapKey;
		}
	}

	/**
	 * Set the order by clause used to order the fetching or the data for this
	 * list, set or map.
	 */
	public void setFetchOrderBy(String orderBy) {
		if (orderBy != null && orderBy.length() > 0) {
			fetchOrderBy = orderBy;
		}
	}

}
