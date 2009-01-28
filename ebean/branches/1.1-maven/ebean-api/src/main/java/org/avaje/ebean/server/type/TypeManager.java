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
 * Convert an Object to the required type.
 */
public interface TypeManager {

	/**
	 * Convert to the appropriate type using java.sql.Types.
	 */
	public Object convert(Object value, int dbType);

	/**
	 * Register a ScalarType with the system.
	 */
	public void add(ScalarType scalarType);

	/**
	 * Return the ScalarType for a given jdbc type.
	 * 
	 * @param jdbcType
	 *            as per java.sql.Types
	 */
	public ScalarType getScalarType(int jdbcType);

	/**
	 * Return the ScalarType for a given logical type.
	 */
	public ScalarType getScalarType(Class<?> type);

	/**
	 * For java.util.Date and java.util.Calendar additionally pass the jdbc type
	 * that you would like the ScalarType to map to. This is because these types
	 * can map to different java.sql.Types depending on the property.
	 */
	public ScalarType getScalarType(Class<?> type, int jdbcType);

	/**
	 * Create a ScalarType for an Enum using a mapping (rather than JPA Ordinal
	 * or String which has limitations).
	 */
	public ScalarType createEnumScalarType(Class<?> enumType);
}
