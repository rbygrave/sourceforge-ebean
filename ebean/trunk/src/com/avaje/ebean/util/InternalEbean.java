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
package com.avaje.ebean.util;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.bean.ObjectGraphNode;

/**
 * Provides non-public API for Serialization support.
 * <p>
 * Implemented by Client and Server implementations of EbeanServer.
 * </p>
 */
public interface InternalEbean extends EbeanServer {

//	/**
//	 * Returns the Class that implements the EntityBean interface for a given
//	 * base class. Used by the serialization mechanism and otherwise not
//	 * expected to be that useful.
//	 * <p>
//	 * Refer to BeanObjectInputStream and BeanObjectOutputStream.
//	 * </p>
//	 * 
//	 * @param className
//	 *            the name of the entity bean class.
//	 * @param methodInfo
//	 *            information used to provide
//	 */
//	public Class<?> resolve(String className, MethodInfo methodInfo);
//
//	/**
//	 * Return the MethodInfo for a given className.
//	 */
//	public MethodInfo getMethodInfo(String className);

	/**
	 * For BeanCollections to call for lazy loading themselves.
	 * 
	 * @param profilePoint
	 *            the profilePoint is only used when profiling is on. It
	 *            contains the location of the original queryHash and
	 *            codeStackHash to link the profile back to.
	 */
	public void lazyLoadMany(Object parentBean, String propertyName, ObjectGraphNode profilePoint);

	/**
	 * Lazy load an entity bean.
	 * 
	 * @param bean
	 *            the entity bean to lazy load.
	 * @param collector
	 *            the collector is only used when profiling is on. It contains
	 *            the profilePoint of the original queryHash and codeStackHash
	 *            to link the profile back to.
	 */
	public void lazyLoadBean(Object bean, NodeUsageCollector collector);

}
