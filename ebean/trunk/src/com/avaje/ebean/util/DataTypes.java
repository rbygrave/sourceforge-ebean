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
package com.avaje.ebean.util;

/**
 * Logical datatypes beyond the ones in java.sql.Types.
 * <p>
 * Note that types that exist in here should be handled by the TypeConverter. If
 * a type is added here DefaultTypeConverter should potentially be modified to
 * handle the type.
 * </p>
 */
public class DataTypes {

	/**
	 * Type code for java.util.Calendar.
	 */
	public static final int UTIL_CALENDAR = -999998986;

	/**
	 * Type code for java.util.Date.
	 */
	public static final int UTIL_DATE = -999998988;

	/**
	 * Type code for java.math.BigInteger.
	 */
	public static final int MATH_BIGINTEGER = -999998987;

	/**
	 * Type code for an Enum type.
	 */
	public static final int ENUM = -999998989;


}
