/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebeaninternal.server.deploy;

import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebeaninternal.server.reflect.BeanReflectSetter;

/**
 * A place holder for BeanReflectSetter that should never be called.
 * <p>
 * This is for properties of classes that are abstract and at the root
 * of an inheritance hierarchy.
 * </p>
 * @author rbygrave
 */
public class ReflectSetter {

	/**
	 * Creates place holder objects that should never be called.
	 */
	public static BeanReflectSetter create(DeployBeanProperty prop) {
		return new NeverCalled(prop.getFullBeanName());
	}
	
	public static class NeverCalled implements BeanReflectSetter {

		private final String property;
		
		public NeverCalled(String property){
			this.property = property;
		}
		
		public void set(Object bean, Object value) {
			throw new RuntimeException("Should never be called on "+property);
		}

		public void setIntercept(Object bean, Object value) {
			throw new RuntimeException("Should never be called on "+property);
		}
				
	}
}
