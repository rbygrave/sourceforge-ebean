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
package com.avaje.ebean.bean;

/**
 * Bean that is aware of EntityBeanIntercept.
 * <p>
 * This interface and implementation of these methods is added to Entity Beans
 * via instrumentation. These methods have a funny _ebean_ prefix to avoid any
 * clash with normal methods these beans would have. These methods are not for
 * general application consumption.
 * </p>
 */
public interface EntityBean {

	/**
	 * Return the enhancement marker value.
	 * <p>
	 * This is the class name of the enhanced class and used to check that
	 * all entity classes are enhanced (specifically not just a super class). 
	 * </p>
	 */
	public String _ebean_getMarker();
	
	/**
	 * Generated method that sets the loaded state on all the embedded beans
	 * on this entity bean by using EntityBeanIntercept.setEmbeddedLoaded(Object o); 
	 */
	public void _ebean_setEmbeddedLoaded();
	
	/**
	 * Return the intercept for this object.
	 */
	public EntityBeanIntercept _ebean_getIntercept();

	/**
	 * Create a copy of this entity bean.
	 * <p>
	 * This occurs when a bean is changed. The copy represents the bean as it
	 * was initially (oldValues) before any changes where made. This is used for
	 * optimistic concurrency control.
	 * </p>
	 */
	public Object _ebean_createCopy();

	/**
	 * Return the fields in their index order.
	 */
	public String[] _ebean_getFieldNames();

	/**
	 * Set the value of a field of an entity bean of this type.
	 * <p>
	 * Note that using this method bypasses any interception that otherwise
	 * occurs on entity beans. That means lazy loading and oldValues creation.
	 * </p>
	 * 
	 * @param fieldIndex
	 *            the index of the field
	 * @param entityBean
	 *            the entityBean of this type to modify
	 * @param value
	 *            the value to set
	 */
	public void _ebean_setField(int fieldIndex, Object entityBean, Object value);

	/**
	 * Set the field value with interception.
	 */
	public void _ebean_setFieldIntercept(int fieldIndex, Object entityBean, Object value);

	/**
	 * Return the value of a field from an entity bean of this type.
	 * <p>
	 * Note that using this method bypasses any interception that otherwise
	 * occurs on entity beans. That means lazy loading.
	 * </p>
	 * 
	 * @param fieldIndex
	 *            the index of the field
	 * @param entityBean
	 *            the entityBean to get the value from
	 */
	public Object _ebean_getField(int fieldIndex, Object entityBean);

	/**
	 * Return the field value with interception.
	 */
	public Object _ebean_getFieldIntercept(int fieldIndex, Object entityBean);

}
