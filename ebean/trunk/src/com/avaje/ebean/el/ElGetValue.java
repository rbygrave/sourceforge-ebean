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
package com.avaje.ebean.el;

import com.avaje.ebean.server.deploy.BeanProperty;

/**
 * The expression language getter method.
 */
public interface ElGetValue extends ElPropertyDeploy {

	public boolean isDeployOnly();
	
	public String getPrefix();

	public String getName();
		
	public String getDbColumn();
	
	public BeanProperty getBeanProperty();
	
	public String getDeployProperty();
	
	/**
	 * Return the value from a given entity bean.
	 */
	public Object elGetValue(Object bean);

	/**
	 * Convert the value to the expected type.
	 * <p>
	 * Typically useful for converting strings to the appropriate number type
	 * etc.
	 * </p>
	 */
	public Object elConvertType(Object value);
}
