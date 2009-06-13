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
package com.avaje.ebean.server.type;

import java.sql.Types;

import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.util.BasicTypeConverter;

/**
 * Helper to create some default ScalarType objects for Booleans,
 * java.util.Date, java.util.Calendar etc.
 */
public class DefaultTypeFactory {

	final ServerConfig serverConfig;

	public DefaultTypeFactory(ServerConfig serverConfig) {
		this.serverConfig = serverConfig;
	}

	/**
	 * Create the ScalarType for mapping Booleans. For some databases this is a
	 * native data type and for others Booleans will be converted to Y/N or 0/1
	 * etc.
	 */
	public ScalarType createBoolean() {


		int booleanDbType = serverConfig.getDatabasePlatform().getBooleanDbType();
		
		if (booleanDbType == Types.BOOLEAN){
			return new ScalarTypeBoolean.Native();			
		}
		
		// Some dbs use BIT e.g. MySQL
		if (booleanDbType == Types.BIT){
			return new ScalarTypeBoolean.BitBoolean();
		}
		
	
		String falseValue = serverConfig.getDatabaseBooleanFalse();
		String trueValue = serverConfig.getDatabaseBooleanTrue();

		if (falseValue == null || trueValue == null){
			// assume native boolean support
			return new ScalarTypeBoolean.Native();
		}
		
		try {

			Integer intTrue = BasicTypeConverter.toInteger(trueValue);
			Integer intFalse = BasicTypeConverter.toInteger(falseValue);

			return new ScalarTypeBoolean.IntBoolean(intTrue, intFalse);

		} catch (NumberFormatException e){
		}
		
		if (falseValue == null || trueValue == null) {
			// the JDBC driver/Database supports Booleans
			return new ScalarTypeBoolean.Native();
		}

		return new ScalarTypeBoolean.StringBoolean(trueValue, falseValue);
	}

	/**
	 * Create the default ScalarType for java.util.Date.
	 */
	public ScalarType createUtilDate() {
		// by default map anonymous java.util.Date to java.sql.Timestamp.
		//String mapType = properties.getProperty("type.mapping.java.util.Date","timestamp");
		int utilDateType = getTemporalMapType("timestamp");

		return createUtilDate(utilDateType);
	}

	/**
	 * Create a ScalarType for java.util.Date explicitly specifying the type to
	 * map to.
	 */
	public ScalarType createUtilDate(int utilDateType) {

		switch (utilDateType) {
		case Types.DATE:
			return new ScalarTypeUtilDate.DateType();
			
		case Types.TIMESTAMP:
			return new ScalarTypeUtilDate.TimestampType();

		default:
			throw new RuntimeException("Invalid type "+utilDateType);
		}
	}

	/**
	 * Create the default ScalarType for java.util.Calendar.
	 */
	public ScalarType createCalendar() {
		// by default map anonymous java.util.Calendar to java.sql.Timestamp.
		//String mapType = properties.getProperty("type.mapping.java.util.Calendar", "timestamp");
		int jdbcType = getTemporalMapType("timestamp");

		return createCalendar(jdbcType);
	}

	/**
	 * Create a ScalarType for java.util.Calendar explicitly specifying the type
	 * to map to.
	 */
	public ScalarType createCalendar(int jdbcType) {

		return new ScalarTypeCalendar(jdbcType);
	}

	private int getTemporalMapType(String mapType) {
		if (mapType.equalsIgnoreCase("date")) {
			return java.sql.Types.DATE;
		}
		return java.sql.Types.TIMESTAMP;
	}

	/**
	 * Create a ScalarType for java.math.BigInteger.
	 */
	public ScalarType createMathBigInteger() {

		return new ScalarTypeMathBigInteger();
	}
}
