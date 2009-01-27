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

import com.avaje.ebean.MapBean;
import com.avaje.ebean.server.reflect.BeanReflectGetter;

/**
 * Getter for a specific property on a MapBean.
 */
public class MapBeanGetter implements BeanReflectGetter {

	private final String propertyName;
	
	/**
	 * Construct for a given property.
	 */
	public MapBeanGetter(String propertyName){
		this.propertyName = propertyName;
	}
	
	/**
	 * Return the property value.
	 */
	@SuppressWarnings("unchecked")
	public Object get(Object mapBean) {
		MapBean m = (MapBean)mapBean;
		return m.get(propertyName);
	}

	public Object getIntercept(Object bean) {
		return get(bean);
	}
	
	
}
