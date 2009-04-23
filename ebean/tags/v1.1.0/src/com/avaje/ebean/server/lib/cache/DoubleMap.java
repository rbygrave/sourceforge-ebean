/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebean.server.lib.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.avaje.ebean.server.lib.cache.DoubleMapCreateValue;

/**
 * DoubleMap provides a Map like object for gets and will automatically create
 * an object as required (the initial get returns null).
 * <p>
 * It provides single threaded automatic creation (and no put support). This
 * means that values are automatically created and put into the DoubleMap but
 * can never be updated or removed.
 * </p>
 * <p>
 * When objects are retrieved by get() this should mostly occur via a
 * ConcurrentHashMap.get(). When this fails then a fully synchronized
 * get/create/put is used. To support this there is actually 2 maps used with
 * one being accessed in a fully synchronised manor.
 * </p>
 * <p>
 * An example application of DoubleMap is for use with Ebean where deployment
 * information is built concurrently and cached. The deployment information does
 * not change, and is read mostly concurrently.
 * </p>
 */
public class DoubleMap<K, V> {

	/**
	 * For fast concurrent reads (hopefully with high hit ratio).
	 */
	private final ConcurrentHashMap<K, V> concMap = new ConcurrentHashMap<K, V>();

	/**
	 * For fully synchronized get/create/put when concMap returned null.
	 */
	private final HashMap<K, V> synchMap = new HashMap<K, V>();

	/**
	 * Used to automatically create the value when it does not yet exist.
	 */
	private final DoubleMapCreateValue<K, V> createEntry;

	/**
	 * Construct with the object used to automatically create new values.
	 */
	public DoubleMap(DoubleMapCreateValue<K, V> createEntry) {
		this.createEntry = createEntry;
	}

	/**
	 * returns values from the concurrent map.
	 * <p>
	 * Supported as per ConcurrentHashMap.values();
	 * </p>
	 */
	public Collection<V> values() {
		return concMap.values();
	}

	/**
	 * returns entries from the concurrent map.
	 * <p>
	 * Supported as per ConcurrentHashMap.entrySet();
	 * </p>
	 */
	public Set<Entry<K, V>> entrySet() {
		return concMap.entrySet();
	}

	/**
	 * Return a value given a key (with automatic creation).
	 * <p>
	 * This will always return a value. If the value does not yet exist then it
	 * will be created automatically. The creation is performed in a single
	 * threaded manor.
	 * </p>
	 */
	public V get(K key) {
		V value = concMap.get(key);
		if (value != null) {
			return value;
		}
		return getWithCreate(key);
	}

	/**
	 * Synchronized get/create/put of the value.
	 */
	protected V getWithCreate(K key) {
		synchronized (this) {
			V value = synchMap.get(key);
			if (value == null) {
				value = createEntry.createValue(key);
				synchMap.put(key, value);
				concMap.put(key, value);

				// support for recursive creation
				createEntry.postPut(value);
			}
			return value;
		}
	}

}
