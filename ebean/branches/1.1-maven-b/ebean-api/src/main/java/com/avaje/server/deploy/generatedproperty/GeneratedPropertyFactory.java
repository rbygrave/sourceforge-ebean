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
package com.avaje.ebean.server.deploy.generatedproperty;

import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;

/**
 * Creates GeneratedProperty for counters, insert timestamps and update
 * timestamps.
 */
public interface GeneratedPropertyFactory {

	/**
	 * Create a counter GeneratedProperty.
	 * <p>
	 * This is used to support version properties. (for optimistic concurrency
	 * checking based on counters).
	 * </p>
	 */
	public GeneratedProperty createCounter(DeployBeanProperty prop);

	/**
	 * Create a insert timestamp GeneratedProperty.
	 * <p>
	 * This maps to a property that has the 'inserted timestamp' set when a bean
	 * is first persisted.
	 * </p>
	 */
	public GeneratedProperty createInsertTimestamp(DeployBeanProperty prop);

	/**
	 * Create a update timestamp GeneratedProperty.
	 * <p>
	 * This maps to a property that has the 'updated timestamp' set whenever a
	 * bean persisted.
	 * </p>
	 * <p>
	 * This is used to support version properties. (for optimistic concurrency
	 * checking based on timestamps).
	 * </p>
	 */
	public GeneratedProperty createUpdateTimestamp(DeployBeanProperty prop);

}
