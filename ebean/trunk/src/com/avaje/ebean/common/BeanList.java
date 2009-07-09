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
package com.avaje.ebean.common;

import java.io.ObjectStreamException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.InternalEbean;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.SerializeControl;

/**
 * List capable of lazy loading.
 */
public final class BeanList<E> implements List<E>, BeanCollection<E> {

	private static final long serialVersionUID = 7594954368722184476L;

	/**
	 * The EbeanServer this is associated with. (used for lazy fetch).
	 */
	private transient InternalEbean internalEbean;
	

	private final transient ObjectGraphNode profilePoint;
	
	/**
	 * The owning bean (used for lazy fetch).
	 */
	private final Object ownerBean;

	/**
	 * The name of this property in the owning bean (used for lazy fetch).
	 */
	private final String propertyName;
	
	/**
	 * The underlying List implementation.
	 */
	private List<E> list;

	/**
	 * Can be false when a background thread is used to continue the fetch the
	 * rows. It will set this to true when it is finished. If no background
	 * thread is used then this should already be true.
	 */
	private boolean finishedFetch = true;

	/**
	 * Flag set to true if rows are limited by firstRow maxRows and more rows
	 * exist. For use by client to enable 'next' for paging.
	 */
	private boolean hasMoreRows;
	
	/**
	 * Specify the underlying List implementation.
	 */
	public BeanList(List<E> list) {
		this.list = list;
		this.profilePoint = null;
		this.propertyName = null;
		this.ownerBean = null;
	}

	/**
	 * Uses an ArrayList as the underlying List implementation.
	 */
	public BeanList() {
		this(new ArrayList<E>());
	}

	/**
	 * Used to create deferred fetch proxy.
	 */	
	public BeanList(InternalEbean internalEbean, Object ownerBean, String propertyName, ObjectGraphNode profilePoint) {
		this.internalEbean = internalEbean;
		this.ownerBean = ownerBean;
		this.propertyName = propertyName;
		this.profilePoint = profilePoint;
	}

	Object readResolve() throws ObjectStreamException {
		if (SerializeControl.isVanillaCollections()) {
			return list;
		}
		return this;
	}

	Object writeReplace() throws ObjectStreamException {
		if (SerializeControl.isVanillaCollections()) {
			return list;
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	public void internalAdd(Object bean) {
		list.add((E) bean);
	}

	private void init() {
		if (list == null && internalEbean != null){
			//InternalEbean eb = (InternalEbean)Ebean.getServer(serverName);
			internalEbean.lazyLoadMany(ownerBean, propertyName, profilePoint);
		}
	}

	/**
	 * Set the actual underlying list.
	 * <p>
	 * This is primarily for the deferred fetching function.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	public void setActualList(List<?> list){
		this.list = (List<E>)list;
	}
	
	/**
	 * Return the actual underlying list.
	 */
	public List<E> getActualList() {
		return list;
	}

	public Iterator<?> getActualDetails() {
		return list.iterator();
	}

	/**
	 * Returns the underlying list.
	 */
	public Object getActualCollection() {
		return list;
	}

	/**
	 * Return true if the underlying list is populated.
	 */
	public boolean isPopulated() {
		return list != null;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("BeanList ");
		if (list == null) {
			sb.append("deferred ");

		} else {
			sb.append("size[").append(list.size()).append("] ");
			sb.append("hasMoreRows[").append(hasMoreRows).append("] ");
			sb.append("list").append(list).append("");
		}
		return sb.toString();
	}

	/**
	 * Equal if obj is a List and equal in a list sense.
	 * <p>
	 * Specifically obj does not need to be a BeanList but any list. This does
	 * not use the FindMany, fetchedMaxRows or finishedFetch properties in the
	 * equals test.
	 * </p>
	 */
	public boolean equals(Object obj) {
		init();
		return list.equals(obj);
	}

	public int hashCode() {
		init();
		return list.hashCode();
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
	// proxy method for List
	// -----------------------------------------------------//

	public void add(int index, E element) {
		init();
		if (modifyListening) {
			modifyAddition(element);
		}
		list.add(index, element);
	}

	public boolean add(E o) {
		init();
		if (modifyListening) {
			if (list.add(o)){
				modifyAddition(o);				
				return true;
			} else {
				return false;
			}
		}
		return list.add(o);
	}

	public boolean addAll(Collection<? extends E> c) {
		init();
		if (modifyListening) {
			// all elements in c are added (no contains checking)
			getModifyHolder().modifyAdditionAll(c);
		}
		return list.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		init();
		if (modifyListening) {
			// all elements in c are added (no contains checking)
			getModifyHolder().modifyAdditionAll(c);
		}
		return list.addAll(index, c);
	}

	public void clear() {
		init();
		list.clear();
	}

	public boolean contains(Object o) {
		init();
		return list.contains(o);
	}

	public boolean containsAll(Collection<?> c) {
		init();
		return list.containsAll(c);
	}

	public E get(int index) {
		init();
		return list.get(index);
	}

	public int indexOf(Object o) {
		init();
		return list.indexOf(o);
	}

	public boolean isEmpty() {
		init();
		return list.isEmpty();
	}

	public Iterator<E> iterator() {
		init();
		if (modifyListening) {
			Iterator<E> it = list.iterator();
			return new ModifyIterator<E>(this, it);
		}
		return list.iterator();
	}

	public int lastIndexOf(Object o) {
		init();
		return list.lastIndexOf(o);
	}

	public ListIterator<E> listIterator() {
		init();
		if (modifyListening) {
			ListIterator<E> it = list.listIterator();
			return new ModifyListIterator<E>(this, it);
		}
		return list.listIterator();
	}

	public ListIterator<E> listIterator(int index) {
		init();
		if (modifyListening) {
			ListIterator<E> it = list.listIterator(index);
			return new ModifyListIterator<E>(this, it);
		}
		return list.listIterator(index);
	}

	public E remove(int index) {
		init();
		if (modifyListening) {
			E o = list.remove(index);
			modifyRemoval(o);
			return o;
		}
		return list.remove(index);
	}

	public boolean remove(Object o) {
		init();
		if (modifyListening) {
			boolean isRemove = list.remove(o);
			if (isRemove) {
				modifyRemoval(o);
			}
			return isRemove;
		}
		return list.remove(o);
	}

	public boolean removeAll(Collection<?> c) {
		init();
		if (modifyListening) {
			boolean changed = false;
			Iterator<?> it = c.iterator();
			while (it.hasNext()) {
				Object o = (Object) it.next();
				if (list.remove(o)) {
					modifyRemoval(o);
					changed = true;
				}
			}
			return changed;
		}
		return list.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		init();
		if (modifyListening) {
			boolean changed = false;
			Iterator<E> it = list.iterator();
			while (it.hasNext()) {
				Object o = (Object) it.next();
				if (!c.contains(o)) {
					it.remove();
					modifyRemoval(o);
					changed = true;
				}
			}
			return changed;
		}
		return list.retainAll(c);
	}

	public E set(int index, E element) {
		init();
		if (modifyListening) {
			E o = list.set(index, element);
			modifyAddition(element);
			modifyRemoval(o);
			return o;
		}
		return list.set(index, element);
	}

	public int size() {
		init();
		return list.size();
	}

	public List<E> subList(int fromIndex, int toIndex) {
		init();
		if (modifyListening) {
			return new ModifyList<E>(this, list.subList(fromIndex, toIndex));
		}
		return list.subList(fromIndex, toIndex);
	}

	public Object[] toArray() {
		init();
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		init();
		return list.toArray(a);
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
		if (modifyHolder == null){
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

	public void modifyReset() {
		if (modifyHolder != null){
			modifyHolder.reset();
		}
	}
	
	public Set<E> getModifyAdditions() {
		if (modifyHolder == null){
			return null;
		} else {
			return modifyHolder.getModifyAdditions();
		}
	}

	public Set<E> getModifyRemovals() {
		if (modifyHolder == null){
			return null;
		} else {
			return modifyHolder.getModifyRemovals();
		}
	}

}
