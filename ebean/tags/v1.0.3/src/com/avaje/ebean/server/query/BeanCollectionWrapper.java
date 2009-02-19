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
package com.avaje.ebean.server.query;

import java.util.Collection;
import java.util.Map;

import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.query.RelationalQuery;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.core.RelationalQueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.ManyType;
import com.avaje.ebean.server.util.BeanCollectionFactory;
import com.avaje.ebean.server.util.BeanCollectionParams;

/**
 * Wraps a BeanCollection with helper methods to add beans.
 * <p>
 * Helps adding the bean to the underlying set list or map.
 * </p>
 */
public class BeanCollectionWrapper {

	/**
	 * Flag set if this builds a Map rather than a Collection.
	 */
	boolean isMap = false;

	/**
	 * The number of rows added.
	 */
	int rowCount = 0;

	/**
	 * The type.
	 */
	ManyType manyType;

	/**
	 * A property name used as key for a Map.
	 */
	String mapKey;

	/**
	 * The actual BeanCollection.
	 */
	BeanCollection<?> beanColl;

	/**
	 * Collection type of BeanCollection.
	 */
	Collection<Object> collection;

	/**
	 * Map type of BeanCollection.
	 */
	Map<Object,Object> map;

	/**
	 * The associated BeanDescriptor.
	 */
	BeanDescriptor desc;

	/**
	 * The find this collection is for.
	 */
	OrmQuery<?> find;

	/**
	 * Params used to create the Collection.
	 */
	BeanCollectionParams buildParams;

	public BeanCollectionWrapper(RelationalQueryRequest request) {

		RelationalQuery q = request.getQuery();
		this.manyType = request.getManyType();
		//this.find = request.getQuery();
		this.mapKey = q.getMapKey();

		// Note that modifyListening is always false. This is because it only
		// relates
		// to ManyToMany collections. Which can only be fetched as a reference
		// or as
		// find.setIncludes() and can not be fetched against in their own right.
		this.buildParams = new BeanCollectionParams(manyType, q.getInitialCapacity(), false);
		init();
	}
	
	/**
	 * Create based on a Find.
	 */
	public BeanCollectionWrapper(QueryRequest request) {

		this.manyType = request.getManyType();
		this.find = request.getQuery();
		this.mapKey = find.getMapKey();

		// Note that modifyListening is always false. This is because it only
		// relates
		// to ManyToMany collections. Which can only be fetched as a reference
		// or as
		// find.setIncludes() and can not be fetched against in their own right.
		this.buildParams = new BeanCollectionParams(manyType, find.getInitialCapacity(), false);
		init();
	}

	/**
	 * Create based on a ManyType and mapKey. Note the mapKey is only used if
	 * the manyType is a Map.
	 * <p>
	 * modifyListening is set to true if this is a collection used to hold
	 * ManyToMany associated objects.
	 * </p>
	 */
	public BeanCollectionWrapper(BeanPropertyAssocMany manyProp) {
		this.manyType = manyProp.getManyType();
		this.mapKey = manyProp.getMapKey();
		this.desc = manyProp.getTargetDescriptor();
		
		this.buildParams = new BeanCollectionParams(manyType);

		init();
	}


	
	/**
	 * Initialise the underlying beanCollection.
	 */
	@SuppressWarnings("unchecked")
	protected void init() {
		beanColl = createBeanCollection();

		isMap = manyType.isMap();
		if (isMap) {
			map = (Map) beanColl;
		} else {
			collection = (Collection) beanColl;
		}
	}

	/**
	 * Set the BeanDescriptor for the type of beans being added to this
	 * collection. This is used to determine the key value for adding beans to a
	 * Map.
	 */
	public void setBeanDescriptor(BeanDescriptor desc) {
		this.desc = desc;
	}

	/**
	 * Return the underlying BeanCollection.
	 */
	public BeanCollection<?> getBeanCollection() {
		return beanColl;
	}

	/**
	 * Create a BeanCollection of the correct type.
	 */
	public BeanCollection<?> createBeanCollection() {
		return BeanCollectionFactory.create(buildParams);
	}

	/**
	 * Return true if this wraps a Map rather than a set or list.
	 */
	public boolean isMap() {
		return isMap;
	}

	/**
	 * Return the number of rows added to this wrapper.
	 */
	public int size() {
		return rowCount;
	}

	/**
	 * Add the bean to the collection held in this wrapper.
	 */
	public void add(Object bean) {
		add(bean, beanColl);
	}

	/**
	 * Add the bean to the collection passed.
	 * 
	 * @param bean
	 *            the bean to add
	 * @param collection
	 *            the collection or map to add the bean to
	 */
	@SuppressWarnings("unchecked")
	public void add(Object bean, Object collection) {
		if (bean == null) {
			return;
		}
		rowCount++;
		if (isMap) {
			Object keyValue = null;
			if (mapKey != null) {
				// use the value for the property
				keyValue = desc.getValue(bean, mapKey);
			} else {
				// use the uniqueId for this
				keyValue = desc.getId(bean);
			}

			Map mapColl = (Map) collection;
			mapColl.put(keyValue, bean);
		} else {
			((Collection) collection).add(bean);
		}
	}

	/**
	 * Specifically add to a Collection.
	 */
	public void addToCollection(Object bean) {
		collection.add(bean);
	}

	/**
	 * Specifically add to this as a Map with a known key.
	 */
	public void addToMap(Object bean, Object key) {
		map.put(key, bean);
	}

}
