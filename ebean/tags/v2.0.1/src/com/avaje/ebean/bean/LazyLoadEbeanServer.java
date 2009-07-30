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

import com.avaje.ebean.EbeanServer;

/**
 * API for Client and Server implementations of EbeanServer.
 */
public interface LazyLoadEbeanServer extends EbeanServer {

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
