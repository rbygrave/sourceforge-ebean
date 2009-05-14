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
package com.avaje.ebean.server.deploy;

import com.avaje.ebean.server.type.TypeManager;

/**
 * Owner attached to a BeanDescriptor.
 * <p>
 * Provides methods to a BeanDescriptor that hook back to more server-wide
 * 'services'.
 * </p>
 */
public interface BeanDescriptorOwner {

	/**
	 * Return the name of the server/database.
	 */
	public String getServerName();

	/**
	 * Return the BeanManager for a given class.
	 */
	public <T> BeanManager<T> getBeanManager(Class<T> entityType);

	/**
	 * Return the BeanDescriptor for a given class.
	 * <p>
	 * This is slightly special in that it bypasses the BeanManager, so it can
	 * be used in the process of creating BeanManager such as defining join
	 * information (JoinTree) etc.
	 * </p>
	 */
	public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> entityType);

	/**
	 * Return the TypeConverter for the server.
	 */
	public TypeManager getTypeManager();

	/**
	 * Convert a Object using the type (typically type as per java.sql.Types).
	 */
	public Object convert(Object v, int type);

}
