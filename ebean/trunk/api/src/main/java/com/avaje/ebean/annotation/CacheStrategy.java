/**
 * Copyright (C) 2009  Robin Bygrave
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
package com.avaje.ebean.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.avaje.ebean.Query;

/**
 * Specify the default cache use specific entity type.
 */
@Target( { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheStrategy {

	/**
	 * Set to true then the bean cache will be used unless explicitly stated
	 * not to in a query via {@link Query#setUseCache(boolean)}.
	 */
	boolean useBeanCache() default true;

	/**
	 * Set to true means by the beans returned from the cache will be treated as
	 * readOnly and this means they can be safely shared by many users.
	 * <p>
	 * If this is false then a copy of the bean is given back to the application
	 * and so the application code that modify that bean.
	 * </p>
	 * <p>
	 * If you try to modify a readOnly bean it will throw an exception.
	 * </p>
	 */
	boolean readOnly() default false;

};
