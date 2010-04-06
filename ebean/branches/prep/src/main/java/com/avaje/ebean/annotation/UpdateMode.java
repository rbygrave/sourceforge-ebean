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
 * Specify the update mode for the specific entity type.
 * <p>
 * Control whether all 'loaded' properties are included in an Update or
 * whether just properties that have changed will be included in the update.
 * </p>
 * <p>
 * Note that the default can be set via ebean.properties.
 * </p>
 * <pre>
 * ## Set to update all loaded properties
 * ebean.updateChangesOnly=false
 * </pre>
 */
@Target( { ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface UpdateMode {

	/**
	 * Set to false if you want to include all the 'loaded' properties in the update.
	 * Otherwise, just the properties that have changed will be included in the update.
	 */
	boolean updateChangesOnly() default true;

};
