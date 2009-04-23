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

import com.avaje.ebean.server.plugin.PluginProperties;
import com.avaje.ebean.util.BasicTypeConverter;

/**
 * Helper to create some default ScalarType objects for Booleans,
 * java.util.Date, java.util.Calendar etc.
 */
public class DefaultTypeFactory {

	final PluginProperties properties;

	public DefaultTypeFactory(PluginProperties properties) {
		this.properties = properties;
	}

	/**
	 * Create the ScalarType for mapping Booleans. For some databases this is a
	 * native data type and for others Booleans will be converted to Y/N or 0/1
	 * etc.
	 */
	public ScalarType createBoolean() {

		String sf0 = properties.getProperty("booleanconverter.false", null);
		String st0 = properties.getProperty("booleanconverter.true", null);

		String sFalse = properties.getProperty("type.boolean.false", sf0);
		String sTrue = properties.getProperty("type.boolean.true", st0);

		String type = properties.getProperty("type.boolean.dbtype", "varchar");

		// Some dbs use BIT e.g. MySQL
		if ("bit".equalsIgnoreCase(type)){
			return new ScalarTypeBoolean.BitBoolean();
		}

		if (sFalse == null || sTrue == null) {
			// the JDBC driver/Database supports Booleans
			return new ScalarTypeBoolean.Native();
		}

		String trueValue = sFalse.trim();
		String falseValue = sTrue.trim();

		type = type.toLowerCase();
		if (type.indexOf("char") != -1) {
			// convert to/from string values...
			return new ScalarTypeBoolean.StringBoolean(trueValue, falseValue);
		}
		

		// convert to/from integer values...
		Integer intTrue = BasicTypeConverter.toInteger(sTrue);
		Integer intFalse = BasicTypeConverter.toInteger(sFalse);

		return new ScalarTypeBoolean.IntBoolean(intTrue, intFalse);
	}

	/**
	 * Create the default ScalarType for java.util.Date.
	 */
	public ScalarType createUtilDate() {
		// by default map anonymous java.util.Date to java.sql.Timestamp.
		String mapType = properties.getProperty("type.mapping.java.util.Date",
				"timestamp");
		int utilDateType = getTemporalMapType(mapType);

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
		String mapType = properties.getProperty(
				"type.mapping.java.util.Calendar", "timestamp");
		int jdbcType = getTemporalMapType(mapType);

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
