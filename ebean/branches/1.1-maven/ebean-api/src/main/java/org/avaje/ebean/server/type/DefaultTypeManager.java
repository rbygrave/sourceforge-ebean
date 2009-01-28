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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import org.avaje.ebean.annotation.EnumMapping;
import org.avaje.ebean.server.core.BootupClasses;
import org.avaje.lib.util.FactoryHelper;
import org.avaje.lib.util.StringHelper;
import org.avaje.ebean.server.plugin.PluginProperties;
import org.avaje.lib.log.LogFactory;

/**
 * Default implementation of TypeManager.
 * <p>
 * Manages the list of ScalarType that is available.
 * </p>
 */
public final class DefaultTypeManager implements TypeManager {

	static final Logger logger = LogFactory.get(DefaultTypeManager.class);

	final ConcurrentHashMap<Class<?>, ScalarType> typeMap;

	final ConcurrentHashMap<Integer, ScalarType> nativeMap;

	final DefaultTypeFactory extraTypeFactory;

	final ScalarType charType = new ScalarTypeChar();

	final ScalarType charArrayType = new ScalarTypeCharArray();

	final ScalarType stringType = new ScalarTypeString();

	final ScalarType longVarcharType = new ScalarTypeLongVarchar();

	final ScalarType clobType = new ScalarTypeClob();

	final ScalarType byteType = new ScalarTypeByte();

	final ScalarType byteArrayType = new ScalarTypeByteArray();

	final ScalarType blobType = new ScalarTypeBlob();

	final ScalarType longVarbinaryType = new ScalarTypeLongVarbinary();

	final ScalarType shortType = new ScalarTypeShort();

	final ScalarType integerType = new ScalarTypeInteger();

	final ScalarType longType = new ScalarTypeLong();

	final ScalarType doubleType = new ScalarTypeDouble();

	final ScalarType floatType = new ScalarTypeFloat();

	final ScalarType bigDecimalType = new ScalarTypeBigDecimal();

	final ScalarType timeType = new ScalarTypeTime();

	final ScalarType dateType = new ScalarTypeDate();

	final ScalarType timestampType = new ScalarTypeTimestamp();

	final ScalarType uuidType = new ScalarTypeUUID();

	final PluginProperties properties;

	/**
	 * Create the DefaultTypeManager.
	 */
	public DefaultTypeManager(PluginProperties properties) {
		this.properties = properties;
		this.typeMap = new ConcurrentHashMap<Class<?>, ScalarType>();
		this.nativeMap = new ConcurrentHashMap<Integer, ScalarType>();
		this.extraTypeFactory = new DefaultTypeFactory(properties);

		initialiseStandard();
		initialiseJodaTypes();
		initialiseFromBootupSearch();
		initialiseEnumsWithMapping();
		initialiseFromProperties();
	}

	/**
	 * Register a custom ScalarType.
	 */
	public void add(ScalarType scalarType) {
		synchronized (typeMap) {
			typeMap.put(scalarType.getType(), scalarType);
			logAdd(scalarType);
		}
	}

	protected void logAdd(ScalarType scalarType) {
		if (logger.isLoggable(Level.FINE)) {
			String msg = "ScalarType register [" + scalarType.getClass().getName() + "]";
			msg += " for [" + scalarType.getType().getName() + "]";
			logger.fine(msg);
		}
	}

	/**
	 * Return the ScalarType for the given jdbc type as per java.sql.Types.
	 */
	public ScalarType getScalarType(int jdbcType) {
		return nativeMap.get(jdbcType);
	}

	/**
	 * This can return null if no matching ScalarType is found.
	 */
	public ScalarType getScalarType(Class<?> type) {
		return typeMap.get(type);
	}

	/**
	 * Return a ScalarType for a given class.
	 * <p>
	 * Used for java.util.Date and java.util.Calendar which can be mapped to
	 * different jdbcTypes in a single system.
	 * </p>
	 */
	public ScalarType getScalarType(Class<?> type, int jdbcType) {

		// check for Clob, LongVarchar etc first...
		// the reason being that String maps to multiple jdbc types
		// varchar, clob, longVarchar.
		ScalarType scalarType = getLobTypes(type, jdbcType);
		if (scalarType != null) {
			// it is a specific Lob type...
			return scalarType;
		}

		scalarType = typeMap.get(type);
		if (scalarType != null) {
			if (jdbcType == 0 || scalarType.getJdbcType() == jdbcType) {
				// matching type
				return scalarType;
			} else {
				// sometime like java.util.Date or java.util.Calendar
				// that that does not map to the same jdbc type as the
				// server wide settings.
			}
		}
		// a util Date with jdbcType not matching server wide settings
		if (type.equals(java.util.Date.class)) {
			return extraTypeFactory.createUtilDate(jdbcType);
		}
		// a Calendar with jdbcType not matching server wide settings
		if (type.equals(java.util.Calendar.class)) {
			return extraTypeFactory.createCalendar(jdbcType);
		}

		String msg = "Unmatched ScalarType for " + type + " jdbcType:" + jdbcType;
		throw new RuntimeException(msg);
	}

	/**
	 * Return the types for the known lob types.
	 * <p>
	 * Kind of special case because these map multiple jdbc types to single Java
	 * types - like String - Varchar, LongVarchar, Clob. For this reason I check
	 * for the specific Lob types first before looking for a matching type.
	 * </p>
	 */
	private ScalarType getLobTypes(Class<?> type, int jdbcType) {

		switch (jdbcType) {
		case Types.LONGVARCHAR:
			return longVarcharType;

		case Types.CLOB:
			return clobType;

		case Types.LONGVARBINARY:
			return longVarbinaryType;

		case Types.BLOB:
			return blobType;

		default:
			return null;
		}
	}

	/**
	 * Convert the Object to the required datatype. The
	 * 
	 * @param value
	 *            the Object value
	 * @param toJdbcType
	 *            the type as per java.sql.Types.
	 */
	public Object convert(Object value, int toJdbcType) {
		if (value == null) {
			return null;
		}
		ScalarType type = nativeMap.get(toJdbcType);
		if (type != null) {
			return type.toJdbcType(value);
		}
		return value;
	}

	/**
	 * Initialise ScalarTypes for Enums that use a Mapping.
	 * <p>
	 * That is, Enums that use an explicit mapping rather than the JPA Ordinal
	 * or String types.
	 * </p>
	 */
	protected void initialiseEnumsWithMapping() {

		String prefix = "ebean.type.enum.";

		Iterator<String> it = properties.keys();
		while (it.hasNext()) {
			String key = it.next();
			if (key.startsWith(prefix)) {
				// we have found a enum that uses a mapping
				String enumClassName = key.substring(prefix.length());
				try {
					Class<?> enumType = Class.forName(enumClassName);
					if (enumType.isEnum()) {
						ScalarType scalarType = createEnumScalarType(enumType);
						add(scalarType);
					} else {
						String msg = "Class [" + enumClassName + "] is not an Enum?";
						logger.log(Level.SEVERE, msg);
					}
				} catch (Throwable ex) {
					String msg = "Error creating Enum ScalarType for [" + enumClassName + "]";
					logger.log(Level.SEVERE, msg, ex);
				}
			}
		}
	}

	/**
	 * Create a ScalarType for an Enum that has additional mapping.
	 * <p>
	 * The reason for this is that often in a DB there will be short codes used
	 * such as A,I,N rather than the ACTIVE, INACTIVE, NEW. So there really
	 * needs to be a mapping from the nicely named enumeration values to the
	 * typically much shorter codes used in the DB.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	public ScalarType createEnumScalarType(Class enumType) {

		boolean integerType = false;
		String nameValuePairs = null;
		
		String key = "type.enum." + enumType.getName();
		nameValuePairs = properties.getProperty(key, null);
		if (nameValuePairs == null) {
			// get the mapping information from EnumMapping
			EnumMapping enumMapping = (EnumMapping)enumType.getAnnotation(EnumMapping.class);
			if (enumMapping != null){
				nameValuePairs  = enumMapping.nameValuePairs();
				integerType = enumMapping.integerType();
			}
		}
		
		if (nameValuePairs == null){
			// there are no name value pairs for this mapping...
			return null;
		}

		Map<String, String> nameValueMap = StringHelper.delimitedToMap(nameValuePairs, ",", "=");

		EnumToDbValueMap<?> beanDbMap = EnumToDbValueMap.create(integerType);
		
		Iterator it = nameValueMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			String name = (String) entry.getKey();
			String value = (String) entry.getValue();

			Object enumValue = Enum.valueOf(enumType, name.trim());
			beanDbMap.add(enumValue, value.trim());
		}

		return new ScalarTypeEnumWithMapping(beanDbMap, enumType);
	}

	/**
	 * Automatically find any ScalarTypes by searching through the class path.
	 * <p>
	 * In avaje.properties define a list of packages in which ScalarTypes are
	 * found. This will search for any class that implements the ScalarType
	 * interface and register it with this TypeManager.
	 * </p>
	 */
	protected void initialiseFromBootupSearch() {

		BootupClasses bootupClasses = properties.getBootupClasses();
		List<Class<?>> foundTypes = bootupClasses.getScalarTypes();
		
		for (int i = 0; i < foundTypes.size(); i++) {
			Class<?> cls = foundTypes.get(i);
			try {

				ScalarType scalarType = (ScalarType) cls.newInstance();
				add(scalarType);

			} catch (Exception e) {
				String msg = "Error loading ScalarType [" + cls.getName() + "]";
				logger.log(Level.SEVERE, msg, e);
			}
		}
	}

	/**
	 * Add any ScalarTypes that are registered via ebean.type.0 to ebean.type.99
	 * properties.
	 */
	protected void initialiseFromProperties() {

		for (int i = 0; i < 100; i++) {
			String type = properties.getProperty("type." + i, null);
			try {
				if (type != null) {
					ScalarType scalarType = (ScalarType) FactoryHelper.create(type);
					add(scalarType);
				}
			} catch (Exception ex) {
				String msg = "Error creating ScalarType " + type;
				logger.log(Level.SEVERE, msg, ex);
			}
		}
	}

	/**
	 * Detect if Joda classes are in the classpath and if so
	 * register the Joda data types.
	 */
	protected void initialiseJodaTypes() {
		try {
			// detect if Joda classes are in the classpath
			Class<?> jodaDateTime = Class.forName("org.joda.time.LocalDateTime");
			if (jodaDateTime != null){
				// Joda classes are in the classpath so register the types
				String msg = "Registering Joda data types";
				logger.log(Level.INFO, msg);
				typeMap.put(LocalDateTime.class, new ScalarTypeJodaLocalDateTime());				
				typeMap.put(LocalDate.class, new ScalarTypeJodaLocalDate());				
				typeMap.put(LocalTime.class, new ScalarTypeJodaLocalTime());				
				typeMap.put(DateTime.class, new ScalarTypeJodaDateTime());				
			}
		} catch (ClassNotFoundException e) {
			String msg = "Joda not in classpath so not registering types";
			logger.log(Level.FINE, msg);
		}
	}
	
	/**
	 * Register all the standard types supported. This is the standard JDBC
	 * types plus some other common types such as java.util.Date and
	 * java.util.Calendar.
	 */
	protected void initialiseStandard() {

		ScalarType utilDateType = extraTypeFactory.createUtilDate();
		typeMap.put(java.util.Date.class, utilDateType);

		ScalarType calType = extraTypeFactory.createCalendar();
		typeMap.put(Calendar.class, calType);

		ScalarType mathBigIntType = extraTypeFactory.createMathBigInteger();
		typeMap.put(BigInteger.class, mathBigIntType);

		ScalarType booleanType = extraTypeFactory.createBoolean();
		typeMap.put(Boolean.class, booleanType);
		typeMap.put(boolean.class, booleanType);
		nativeMap.put(Types.BOOLEAN, booleanType);
		
		typeMap.put(UUID.class, uuidType);

		// String types
		typeMap.put(char[].class, charArrayType);
		typeMap.put(char.class, charType);
		typeMap.put(String.class, stringType);
		nativeMap.put(Types.VARCHAR, stringType);
		nativeMap.put(Types.CHAR, stringType);
		nativeMap.put(Types.LONGVARCHAR, longVarcharType);
		nativeMap.put(Types.CLOB, clobType);

		// Binary type
		typeMap.put(byte[].class, byteArrayType);
		nativeMap.put(Types.BINARY, byteArrayType);
		nativeMap.put(Types.VARBINARY, byteArrayType);
		nativeMap.put(Types.LONGVARBINARY, byteArrayType);
		nativeMap.put(Types.BLOB, byteArrayType);

		// Number types
		typeMap.put(Byte.class, byteType);
		typeMap.put(byte.class, byteType);
		nativeMap.put(Types.TINYINT, byteType);

		typeMap.put(Short.class, shortType);
		typeMap.put(short.class, shortType);
		nativeMap.put(Types.SMALLINT, shortType);

		typeMap.put(Integer.class, integerType);
		typeMap.put(int.class, integerType);
		nativeMap.put(Types.INTEGER, integerType);

		typeMap.put(Long.class, longType);
		typeMap.put(long.class, longType);
		nativeMap.put(Types.BIGINT, longType);

		typeMap.put(Double.class, doubleType);
		typeMap.put(double.class, doubleType);
		nativeMap.put(Types.FLOAT, doubleType);// no this is not a bug
		nativeMap.put(Types.DOUBLE, doubleType);

		typeMap.put(Float.class, floatType);
		typeMap.put(float.class, floatType);
		nativeMap.put(Types.REAL, floatType);// no this is not a bug

		typeMap.put(BigDecimal.class, bigDecimalType);
		nativeMap.put(Types.DECIMAL, bigDecimalType);
		nativeMap.put(Types.NUMERIC, bigDecimalType);

		// Temporal types
		typeMap.put(Time.class, timeType);
		nativeMap.put(Types.TIME, timeType);
		typeMap.put(Date.class, dateType);
		nativeMap.put(Types.DATE, dateType);
		typeMap.put(Timestamp.class, timestampType);
		nativeMap.put(Types.TIMESTAMP, timestampType);

	}

}
