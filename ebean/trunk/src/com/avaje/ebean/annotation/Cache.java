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
package com.avaje.ebean.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specify cache options for a given bean.
 * <p>
 * These are hints to the cache implementation. Depending on the cache
 * implementation these options may or may not be used.
 * </p>
 */
@Target( { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Cache {

	/**
	 * Specify the maximum size the cache should get.
	 */
	int maxSize() default 0;

	/**
	 * Specify the maximum a entry can stay in the cache without being accessed.
	 * <p>
	 * This time is specified in milliseconds.
	 * </p>
	 */
	long maxIdleTime() default 0;

	/**
	 * Specify the maximum time an entry can stay in the cache.
	 * <p>
	 * This time is specified in milliseconds.
	 * </p>
	 */
	long maxTimeToLive() default 0;

};
