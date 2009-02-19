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
package com.avaje.ebean.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.util.InternalEbean;

/**
 * Map capable of lazy loading.
 */
public final class BeanMap<K, E> implements Map<K, E>, BeanCollection<E> {

	static final long serialVersionUID = 1748601350011695655L;

	/**
	 * The underlying map implementation.
	 */
	Map<K, E> map;

	/**
	 * Can be false when a background thread is used to continue the fetch the
	 * rows. It will set this to true when it is finished. If no background
	 * thread is used then this should already be true.
	 */
	boolean finishedFetch = true;

	/**
	 * Flag set to true if rows are limited by firstRow maxRows and more rows
	 * exist. For use by client to enable 'next' for paging.
	 */
	boolean hasMoreRows;

	/**
	 * The name of the EbeanServer this is associated with. (used for lazy
	 * fetch).
	 */
	String serverName;

	/**
	 * The owning bean (used for lazy fetch).
	 */
	Object ownerBean;

	/**
	 * The name of this property in the owning bean (used for lazy fetch).
	 */
	String propertyName;

	transient final ObjectGraphNode profilePoint;
	
	/**
	 * Create with a given Map.
	 */
	public BeanMap(Map<K, E> map) {
		this.map = map;
		this.profilePoint = null;
	}

	/**
	 * Create using a underlying LinkedHashMap.
	 */
	public BeanMap() {
		this(new LinkedHashMap<K, E>());
	}

	public BeanMap(String serverName, Object ownerBean, String propertyName, ObjectGraphNode profilePoint) {
		this.serverName = serverName;
		this.ownerBean = ownerBean;
		this.propertyName = propertyName;
		this.profilePoint = profilePoint;
	}

//	Object readResolve() throws ObjectStreamException {
//		if (SerializeControl.isVanillaCollections()) {
//			return map;
//		}
//		return this;
//	}
//
//	Object writeReplace() throws ObjectStreamException {
//		if (SerializeControl.isVanillaCollections()) {
//			return map;
//		}
//		return this;
//	}
	
	public void internalAdd(Object bean) {
		throw new RuntimeException("Not allowed for map");
	}
	
	/**
	 * Return true if the underlying map has been populated. Returns false if it
	 * has a deferred fetch pending.
	 */
	public boolean isPopulated() {
		return map != null;
	}

	//@SuppressWarnings("unchecked")
	private void init() {
		if (map == null) {
			InternalEbean eb = (InternalEbean)Ebean.getServer(serverName);
			eb.lazyLoadMany(ownerBean, propertyName, profilePoint);
		}
	}

	/**
	 * Set the actual underlying map. Used for performing lazy fetch.
	 */
	@SuppressWarnings("unchecked")
	public void setActualMap(Map<?, ?> map) {
		this.map = (Map<K, E>) map;
	}

	/**
	 * Return the actual underlying map.
	 */
	public Map<K, E> getActualMap() {
		return map;
	}

	/**
	 * Returns the map entrySet iterator.
	 * <p>
	 * This is because the key values may need to be set against the details (so
	 * they don't need to be set twice).
	 * </p>
	 */
	public Iterator<?> getActualDetails() {
		return map.entrySet().iterator();
	}

	/**
	 * Returns the underlying map.
	 */
	public Object getActualCollection() {
		return map;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("BeanMap ");
		if (map == null) {
			sb.append("deferred ");

		} else {
			sb.append("size[").append(map.size()).append("]");
			sb.append(" hasMoreRows[").append(hasMoreRows).append("]");
			sb.append(" map").append(map);
		}
		return sb.toString();
	}

	/**
	 * Equal if obj is a Map and equal in a Map sense.
	 */
	public boolean equals(Object obj) {
		init();
		return map.equals(obj);
	}

	public int hashCode() {
		init();
		return map.hashCode();
	}

	// -----------------------------------------------------//
	// The additional methods are here
	// -----------------------------------------------------//

	/**
	 * Set to true if maxRows was hit and there are actually more rows
	 * available.
	 * <p>
	 * Can be used by client code that is paging through results using
	 * setFirstRow() setMaxRows(). If this returns true then the client can
	 * display a 'next' button etc.
	 * </p>
	 */
	public boolean hasMoreRows() {
		return hasMoreRows;
	}

	/**
	 * Set to true when maxRows is hit but there are actually more rows
	 * available. This is set so that client code knows that there is more data
	 * available.
	 */
	public void setHasMoreRows(boolean hasMoreRows) {
		this.hasMoreRows = hasMoreRows;
	}

	/**
	 * Returns true if the fetch has finished. False if the fetch is continuing
	 * in a background thread.
	 */
	public boolean isFinishedFetch() {
		return finishedFetch;
	}

	/**
	 * Set to true when a fetch has finished. Used when a fetch continues in the
	 * background.
	 */
	public void setFinishedFetch(boolean finishedFetch) {
		this.finishedFetch = finishedFetch;
	}

	// -----------------------------------------------------//
	// proxy method for map
	// -----------------------------------------------------//

	public void clear() {
		init();
		map.clear();
	}

	public boolean containsKey(Object key) {
		init();
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		init();
		return map.containsValue(value);
	}

	@SuppressWarnings("unchecked")
	public Set<Entry<K, E>> entrySet() {
		init();
		if (modifyListening) {
			Set<Entry<K, E>> s = map.entrySet();
			// FIXME: Could be a bug here...
			return new ModifySet(this, s);
		}
		return map.entrySet();
	}

	public E get(Object key) {
		init();
		return map.get(key);
	}

	public boolean isEmpty() {
		init();
		return map.isEmpty();
	}

	public Set<K> keySet() {
		init();
		// we don't really care about modifications to the ketSet?
		return map.keySet();
	}

	public E put(K key, E value) {
		init();
		if (modifyListening) {
			Object o = map.put(key, value);
			modifyAddition(value);
			modifyRemoval(o);
		}
		return map.put(key, value);
	}

	@SuppressWarnings("unchecked")
	public void putAll(Map<? extends K, ? extends E> t) {
		init();
		if (modifyListening) {
			Iterator it = t.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				Object o = map.put((K) entry.getKey(), (E) entry.getValue());
				modifyAddition((E) entry.getValue());
				modifyRemoval(o);
			}
		}
		map.putAll(t);
	}

	public E remove(Object key) {
		init();
		if (modifyListening) {
			E o = map.remove(key);
			modifyRemoval(o);
			return o;
		}
		return map.remove(key);
	}

	public int size() {
		init();
		return map.size();
	}

	public Collection<E> values() {
		init();
		if (modifyListening) {
			Collection<E> c = map.values();
			return new ModifyCollection<E>(this, c);
		}
		return map.values();
	}

	// ---------------------------------------------------------
	// Support for modify additions deletions etc
	// ---------------------------------------------------------

	ModifyHolder<E> modifyHolder;

	boolean modifyListening;

	/**
	 * set modifyListening to be on or off.
	 */
	public void setModifyListening(boolean modifyListening) {
		this.modifyListening = modifyListening;
		if (modifyListening){
			// lose any existing modifications
			modifyHolder = null;
		}
	}

	private ModifyHolder<E> getModifyHolder() {
		if (modifyHolder == null) {
			modifyHolder = new ModifyHolder<E>();
		}
		return modifyHolder;
	}

	public void modifyAddition(E bean) {
		getModifyHolder().modifyAddition(bean);
	}

	public void modifyRemoval(Object bean) {
		getModifyHolder().modifyRemoval(bean);
	}

	public Set<E> getModifyAdditions() {
		if (modifyHolder == null) {
			return null;
		} else {
			return modifyHolder.getModifyAdditions();
		}
	}

	public Set<E> getModifyRemovals() {
		if (modifyHolder == null) {
			return null;
		} else {
			return modifyHolder.getModifyRemovals();
		}
	}

}
