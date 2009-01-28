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
package org.avaje.ebean.server.reflect;

/**
 * The setter for a given bean property.
 */
public interface BeanReflectSetter {

	/**
	 * Set the property value of a bean.
	 */
	public void set(Object bean, Object value);

	/**
	 * Set the property value of a bean with interception checks.
	 * <p>
	 * This could invoke lazy loading and or oldValues creation.
	 * </p>
	 */
	public void setIntercept(Object bean, Object value);

}
