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
package com.avaje.ebean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.util.BasicTypeConverter;

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
public class SqlRow implements Serializable, Map<String, Object> {

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
	public SqlRow(Map<String, Object> map) {
		this.map = map;
	}

	/**
	 * Create a new MapBean based on a LinkedHashMap with default
	 * initialCapacity (of 16).
	 */
	public SqlRow() {
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
	public SqlRow(int initialCapacity, float loadFactor) {
		this.map = new LinkedHashMap<String, Object>(initialCapacity, loadFactor);
	}

	/**
	 * Return the property names (String).
	 * <p>
	 * Internally this uses LinkedHashMap and so the order of the property names
	 * should be predictable and ordered by the use of LinkedHashMap.
	 * </p>
	 */
	public Iterator<String> keys() {
		return map.keySet().iterator();
	}

	/**
	 * Remove a property from the map. Returns the value of the removed
	 * property.
	 */
	public Object remove(Object name) {
		name = ((String)name).toLowerCase();
		return map.remove(name);
	}

	/**
	 * Return a property value by its name.
	 */
	public Object get(Object name) {
		name = ((String)name).toLowerCase();
		return map.get(name);
	}

	/**
	 * Set a value to a property.
	 */
	public Object put(String name, Object value) {
		return setInternal(name, value);
	}

	/**
	 * Exactly the same as the put method.
	 * <p>
	 * I added this method because it seems more bean like to have get and set
	 * methods.
	 * </p>
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

	/**
	 * Return a property as an Integer.
	 */
	public Integer getInteger(String name) {
		Object val = get(name);
		return BasicTypeConverter.toInteger(val);
	}

	/**
	 * Return a property value as a BigDecimal.
	 */
	public BigDecimal getBigDecimal(String name) {
		Object val = get(name);
		return BasicTypeConverter.toBigDecimal(val);
	}

	/**
	 * Return a property value as a Long.
	 */
	public Long getLong(String name) {
		Object val = get(name);
		return BasicTypeConverter.toLong(val);
	}

	/**
	 * Return the property value as a Double.
	 */
	public Double getDouble(String name) {
		Object val = get(name);
		return BasicTypeConverter.toDouble(val);
	}

	/**
	 * Return the property value as a Float.
	 */
	public Float getFloat(String name) {
		Object val = get(name);
		return BasicTypeConverter.toFloat(val);
	}

	/**
	 * Return a property as a String.
	 */
	public String getString(String name) {
		Object val = get(name);
		return BasicTypeConverter.toString(val);
	}

	/**
	 * Return the property as a java.util.Date.
	 */
	public java.util.Date getUtilDate(String name) {
		Object val = get(name);
		return BasicTypeConverter.toUtilDate(val);
	}

	/**
	 * Return the property as a sql date.
	 */
	public Date getDate(String name) {
		Object val = get(name);
		return BasicTypeConverter.toDate(val);
	}

	/**
	 * Return the property as a sql timestamp.
	 */
	public Timestamp getTimestamp(String name) {
		Object val = get(name);
		return BasicTypeConverter.toTimestamp(val);
	}

	/**
	 * String description of the underlying map.
	 */
	public String toString() {
		return map.toString();
	}

	// ------------------------------------
	// Normal map methods...

	/**
	 * Clear the map.
	 */
	public void clear() {
		map.clear();
	}

	/**
	 * Returns true if the map contains the property.
	 */
	public boolean containsKey(Object key) {
		key = ((String)key).toLowerCase();
		return map.containsKey(key);
	}

	/**
	 * Returns true if the map contains the value.
	 */
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	/**
	 * Returns the entrySet of the map.
	 */
	public Set<Map.Entry<String, Object>> entrySet() {
		return map.entrySet();
	}

	/**
	 * Returns true if the map is empty.
	 */
	public boolean isEmpty() {
		return map.isEmpty();
	}

	/**
	 * Returns the key set of the map.
	 */
	public Set<String> keySet() {
		return map.keySet();
	}

	/**
	 * Put all the values from t into this map.
	 */
	public void putAll(Map<? extends String, ? extends Object> t) {
		map.putAll(t);
	}

	/**
	 * Return the size of the map.
	 */
	public int size() {
		return map.size();
	}

	/**
	 * Return the values from this map.
	 */
	public Collection<Object> values() {
		return map.values();
	}


}
