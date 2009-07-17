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
package com.avaje.ebean.server.query;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.server.core.BasicTypeConverter;

/**
 * Used to return raw SQL query results.
 * <p>
 * Refer to {@link SqlQuery} for examples.
 * </p>
 * <p>
 * There are convenience methods such as getInteger(), getBigDecimal() etc. The
 * reason for these methods is that the values put into this map often come
 * straight from the JDBC resultSet. Depending on the JDBC driver it may put a
 * different type into a given property. For example an Integer, BigDecimal,
 * Double could all be put into a property depending on the JDBC driver used.
 * These convenience methods automatically convert the value as required
 * returning the type you expect.
 * </p>
 */
public class DefaultSqlRow implements SqlRow {

	static final long serialVersionUID = -3120927797041336242L;

	/**
	 * The underlying map of property data.
	 */
	Map<String, Object> map;

	/**
	 * Create with a specific Map implementation.
	 * <p>
	 * The default Map implementation is LinkedHashMap.
	 * </p>
	 */
	public DefaultSqlRow(Map<String, Object> map) {
		this.map = map;
	}

	/**
	 * Create a new MapBean based on a LinkedHashMap with default
	 * initialCapacity (of 16).
	 */
	public DefaultSqlRow() {
		this.map = new LinkedHashMap<String, Object>();
	}

	/**
	 * Create with an initialCapacity and loadFactor.
	 * <p>
	 * The defaults of these are 16 and 0.75.
	 * </p>
	 * <p>
	 * Note that the Map will rehash the contents when the number of keys in
	 * this map reaches its threshold (initialCapacity * loadFactor).
	 * </p>
	 */
	public DefaultSqlRow(int initialCapacity, float loadFactor) {
		this.map = new LinkedHashMap<String, Object>(initialCapacity, loadFactor);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#keys()
	 */
	public Iterator<String> keys() {
		return map.keySet().iterator();
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#remove(java.lang.Object)
	 */
	public Object remove(Object name) {
		name = ((String)name).toLowerCase();
		return map.remove(name);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#get(java.lang.Object)
	 */
	public Object get(Object name) {
		name = ((String)name).toLowerCase();
		return map.get(name);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#put(java.lang.String, java.lang.Object)
	 */
	public Object put(String name, Object value) {
		return setInternal(name, value);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#set(java.lang.String, java.lang.Object)
	 */
	public Object set(String name, Object value) {
		return setInternal(name, value);
	}

	private Object setInternal(String name, Object newValue) {
		// MapBean properties are always lowercase
		name = name.toLowerCase();

		// valueList = null;
		return map.put(name, newValue);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getInteger(java.lang.String)
	 */
	public Integer getInteger(String name) {
		Object val = get(name);
		return BasicTypeConverter.toInteger(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getBigDecimal(java.lang.String)
	 */
	public BigDecimal getBigDecimal(String name) {
		Object val = get(name);
		return BasicTypeConverter.toBigDecimal(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getLong(java.lang.String)
	 */
	public Long getLong(String name) {
		Object val = get(name);
		return BasicTypeConverter.toLong(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getDouble(java.lang.String)
	 */
	public Double getDouble(String name) {
		Object val = get(name);
		return BasicTypeConverter.toDouble(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getFloat(java.lang.String)
	 */
	public Float getFloat(String name) {
		Object val = get(name);
		return BasicTypeConverter.toFloat(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getString(java.lang.String)
	 */
	public String getString(String name) {
		Object val = get(name);
		return BasicTypeConverter.toString(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getUtilDate(java.lang.String)
	 */
	public java.util.Date getUtilDate(String name) {
		Object val = get(name);
		return BasicTypeConverter.toUtilDate(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getDate(java.lang.String)
	 */
	public Date getDate(String name) {
		Object val = get(name);
		return BasicTypeConverter.toDate(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#getTimestamp(java.lang.String)
	 */
	public Timestamp getTimestamp(String name) {
		Object val = get(name);
		return BasicTypeConverter.toTimestamp(val);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#toString()
	 */
	public String toString() {
		return map.toString();
	}

	// ------------------------------------
	// Normal map methods...

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#clear()
	 */
	public void clear() {
		map.clear();
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		key = ((String)key).toLowerCase();
		return map.containsKey(key);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#containsValue(java.lang.Object)
	 */
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#entrySet()
	 */
	public Set<Map.Entry<String, Object>> entrySet() {
		return map.entrySet();
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#isEmpty()
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#keySet()
	 */
	public Set<String> keySet() {
		return map.keySet();
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#putAll(java.util.Map)
	 */
	public void putAll(Map<? extends String, ? extends Object> t) {
		map.putAll(t);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#size()
	 */
	public int size() {
		return map.size();
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.ISqlRow#values()
	 */
	public Collection<Object> values() {
		return map.values();
	}


}
