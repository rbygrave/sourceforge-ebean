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

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.util.BasicTypeConverter;

/**
 * [Rename to SqlRow?] Map based EntityBean implementation. Used to dynamically
 * create beans based on a table or nativeSql.
 * <p>
 * Note that MapBeans based on a table can be persisted via {@link Ebean#save(Object)} and
 * {@link Ebean#delete(Object)}. MapBeans based on a nativeSql query can not be persisted until the
 * table to persist to has been set.
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
 * 
 * <pre class="code">        
 * // A &quot;Relational mode&quot; query...
 *          
 * // Get row from oe_order table where primary key = 10
 *          
 * FindByUid find = new FindByUid();
 * find.setTableName(&quot;oe_order&quot;);
 * find.setUid(&quot;10&quot;);
 *         
 * MapBean mapBean = (MapBean)Ebean.find(find);
 *         
 * // get some field data from order 10
 * Integer orderId = mapBean.getInteger(&quot;id&quot;);
 * Integer custId  = mapBean.getInteger(&quot;cust_id&quot;);
 * ...
 *          
 * // update the order status and save
 * mapBean.setString(&quot;status_code&quot;,&quot;SHIPPED&quot;);
 *          
 * Ebean.save(mapBean);
 *          
 * </pre>
 */
public class MapBean implements EntityBean, Cloneable, Serializable, Map<String, Object> {

	static final long serialVersionUID = -3120927797041336241L;

	/**
	 * Table name used when based on a table.
	 */
	String tableName;

	/**
	 * The Id property name if available.
	 */
	String idPropertyName;

	/**
	 * The underlying map of property data.
	 */
	Map<String, Object> map;

	/**
	 * Specific implementation of EntityBeanIntercept.
	 */
	MapBeanIntercept ebeanIntercept;

	/**
	 * Used for equals implementation when id property is null.
	 */
	private Object identityObject;
	
	private String monitor = new String();

	/**
	 * Create with a specific Map implementation.
	 * <p>
	 * The default Map implementation is LinkedHashMap.
	 * </p>
	 */
	public MapBean(Map<String, Object> map) {
		this.map = map;
		ebeanIntercept = new MapBeanIntercept(this);
	}

	/**
	 * Create a new MapBean based on a LinkedHashMap with default
	 * initialCapacity (of 16).
	 */
	public MapBean() {
		this.map = new LinkedHashMap<String, Object>();
		ebeanIntercept = new MapBeanIntercept(this);
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
	public MapBean(int initialCapacity, float loadFactor) {
		this.map = new LinkedHashMap<String, Object>(initialCapacity, loadFactor);
		ebeanIntercept = new MapBeanIntercept(this);
	}

	/**
	 * Not allowed for MapBeans.
	 */
	public void setEbeanIntercept(EntityBeanIntercept ebi) {
		throw new RuntimeException("Never called");
	}

	/**
	 * No embedded beans on MapBean.
	 */
	public void _ebean_setEmbeddedLoaded() {
		// no embedded bean on MapBean
	}

	/**
	 * Return the class name of this instance.
	 */
	public String _ebean_getMarker() {
		return this.getClass().getName();
	}

	/**
	 * Not used for MapBean.
	 */
	public Object _ebean_getField(int index, Object bean) {
		return null;
	}
	
	/**
	 * Not used for MapBean.
	 */
	public Object _ebean_getFieldIntercept(int index, Object bean) {
		return null;
	}

	/**
	 * Not used for MapBean.
	 */
	public void _ebean_setField(int index, Object bean, Object value) {
	}

	/**
	 * Not used for MapBean.
	 */
	public void _ebean_setFieldIntercept(int index, Object bean, Object value) {
	}

	/**
	 * Not used for MapBean.
	 */
	public String[] _ebean_getFieldNames() {
		return null;
	}

	
	public Object _ebean_createCopy() {
		try {
			// should be ok just being a MapBean rather than
			// a subclass etc as just need get() method support to
			// read the old values out for update or delete
			MapBean old = new MapBean();
			old.map = new LinkedHashMap<String, Object>(map);

			return old;

		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}
	
	/**
	 * Return the EntityBeanIntercept that controls the method interception and
	 * old values creation.
	 */
	public EntityBeanIntercept _ebean_getIntercept() {
		return ebeanIntercept;
	}

	/**
	 * Resets the oldValues back to null and ebeanIntercept 'loaded' to false.
	 * <p>
	 * Use this to force an insert or if you want to set additional data to the
	 * bean without creating the old values.
	 * </p>
	 * <p>
	 * Note that you will need to call ebeanSetLoaded(true) after this for old
	 * values to be created.
	 * </p>
	 */
	public void resetOldValues() {
		ebeanIntercept.resetOldValues();
	}

	/**
	 * Same as {@link #getBaseTable()}
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * Same as {@link #setBaseTable(String)}
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	/**
	 * Set the table name. You need to do this if you want to use this bean to
	 * insert, update or delete to a table. 
	 */
	public void setBaseTable(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * Return the table name for this bean. Used to automatically persist the
	 * bean back to a table using jdbc meta data on the table.
	 */
	public String getBaseTable() {
		return tableName;
	}
	
	/**
	 * Set the ID property name.
	 * <p>
	 * This property name is used by equals to try to do equality based on the
	 * value of the unique id.
	 * </p>
	 * <p>
	 * This is not set for tables with concatenated primary keys.
	 * </p>
	 */
	public void setIdPropertyName(String idPropertyName) {
		this.idPropertyName = idPropertyName;
	}

	/**
	 * Return the ID property name.
	 */
	public String getIdPropertyName() {
		return idPropertyName;
	}

	/**
	 * Return the Id value based on the ID property name.
	 */
	public Object getId() {
		if (idPropertyName != null) {
			return get(idPropertyName);
		}
		return null;
	}

	/**
	 * Equal is based on the id property.
	 * <p>
	 * If the id is null then this uses an object and equality becomes == (same
	 * instance).
	 * </p>
	 */
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o instanceof MapBean) {
			return getIdentityObject().equals(((MapBean) o).getIdentityObject());
		} else {
			return false;
		}
	}

	/**
	 * Return a hashCode for the
	 */
	public int hashCode() {
		return getIdentityObject().hashCode();
	}

	/**
	 * Return the identityObject used for equals.
	 */
	private Object getIdentityObject() {
		synchronized (monitor) {
			// if the identityObject is set then always use it
			if (identityObject != null) {
				return identityObject;
			}
			// if the id is set... always use it
			Object id = getId();
			if (id != null) {
				return id;
			}
			// set the identityObject. Used from now on.
			if (identityObject == null) {
				identityObject = new Object();
			}
		}
		return identityObject;
	}

	/**
	 * Return a clone of this MapBean.
	 */
	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			MapBean newBean = (MapBean) super.clone();
			newBean.map = new LinkedHashMap(map);

			return newBean;

		} catch (CloneNotSupportedException ex) {
			throw new PersistenceException(ex);
		}
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
		Object oldValue = get(name);
		ebeanIntercept.preSetter(name, newValue, oldValue);

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

	/**
	 * Used to create hold and manage the 'old values'.
	 */
	protected class MapBeanIntercept extends EntityBeanIntercept {

		static final long serialVersionUID = -3120927797041336242L;

		MapBean mb;

		protected MapBeanIntercept(MapBean owner) {
			super(owner);
			mb = owner;
		}

		protected void resetOldValues() {
			this.loaded = false;
			this.oldValues = null;
		}

		protected void createOldValues() {
			try {
				// should be ok just being a MapBean rather than
				// a subclass etc as just need get() method support to
				// read the old values out for update or delete
				MapBean old = new MapBean();
				old.map = new LinkedHashMap<String, Object>(map);

				oldValues = old;

			} catch (Exception ex) {
				throw new PersistenceException(ex);
			}
		}
	}

}
