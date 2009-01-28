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
package org.avaje.ebean.server.deploy;

import org.avaje.ebean.MapBean;
import org.avaje.ebean.server.reflect.BeanReflectSetter;

/**
 * Setter for a specific property on a MapBean.
 */
public class MapBeanSetter implements BeanReflectSetter {

	private final String propertyName;
	
	/**
	 * Construct for a given property.
	 */
	public MapBeanSetter(String propertyName){
		this.propertyName = propertyName;
	}
	
	/**
	 * Set the property value.
	 */
	@SuppressWarnings("unchecked")
	public void set(Object mapBean, Object value) {
		MapBean m = (MapBean)mapBean;
		m.set(propertyName, value);
	}

	public void setIntercept(Object bean, Object value) {
		set(bean, value);
	}

}
