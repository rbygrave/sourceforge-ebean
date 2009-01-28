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

import java.util.HashMap;

/**
 * Used to map Bean values to DB values.
 * <p>
 * Useful for building Enum converters where you want to map the DB values an
 * Enum gets converter to.
 * </p>
 * 
 * @param <B>
 *            The Bean value type
 * @param <D>
 *            The DB value type
 */
public class BeanToDbMap<B, D> {

	final HashMap<B, D> keyMap;

	final HashMap<D, B> valueMap;

	final boolean allowNulls;

	/**
	 * Construct with allowNulls defaulting to false.
	 */
	public BeanToDbMap() {
		this(false);
	}

	/**
	 * Construct with allowNulls setting.
	 * <p>
	 * If allowNulls is false then an IllegalArgumentException is thrown by
	 * either the getDBValue or getBeanValue methods if not matching Bean or DB
	 * value is found.
	 * </p>
	 */
	public BeanToDbMap(boolean allowNulls) {
		this.allowNulls = allowNulls;
		keyMap = new HashMap<B, D>();
		valueMap = new HashMap<D, B>();
	}

	/**
	 * Add a bean value and DB value pair.
	 */
	public BeanToDbMap<B, D> add(B beanValue, D dbValue) {
		keyMap.put(beanValue, dbValue);
		valueMap.put(dbValue, beanValue);
		return this;
	}

	/**
	 * Return the DB value given the bean value.
	 */
	public D getDbValue(B beanValue) {
		if (beanValue == null){
			return null;
		}
		D dbValue = keyMap.get(beanValue);
		if (dbValue == null && !allowNulls) {
			String msg = "DB value for " + beanValue + " not found in "+valueMap;
			throw new IllegalArgumentException(msg);
		}
		return dbValue;
	}

	/**
	 * Return the Bean value given the DB value.
	 */
	public B getBeanValue(D dbValue) {
		if (dbValue == null){
			return null;
		}
		B beanValue = valueMap.get(dbValue);
		if (beanValue == null && !allowNulls) {
			String msg = "Bean value for " + dbValue + " not found in "+valueMap;
			throw new IllegalArgumentException(msg);
		}
		return beanValue;
	}
}
