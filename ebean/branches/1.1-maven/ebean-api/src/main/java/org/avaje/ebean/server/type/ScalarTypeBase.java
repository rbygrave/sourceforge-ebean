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
package org.avaje.ebean.server.type;

/**
 * Base ScalarType object.
 */
public abstract class ScalarTypeBase implements ScalarType {

	final Class<?> type;
	final boolean jdbcNative;
	final int jdbcType;
	
	public ScalarTypeBase(Class<?> type, boolean jdbcNative, int jdbcType) {
		this.type = type;
		this.jdbcNative = jdbcNative;
		this.jdbcType = jdbcType;
	}
	
	public boolean isJdbcNative() {
		return jdbcNative;
	}
	
	public int getJdbcType() {
		return jdbcType;
	}
	
	public Class<?> getType() {
		return type;
	}
	
}
