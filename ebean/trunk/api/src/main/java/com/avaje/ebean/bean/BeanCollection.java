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
package com.avaje.ebean.bean;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

/**
 * Lazy loading capable Maps, Lists and Sets.
 * <p>
 * This also includes the ability to listen for additions and removals to or
 * from the Map Set or List. The purpose of gathering the additions and removals
 * is to support persisting ManyToMany objects. The additions and removals
 * become inserts and deletes from the intersection table.
 * </p>
 * <p>
 * Technically this is <em>NOT</em> an extension of
 * <em>java.util.Collection</em>. The reason being that java.util.Map is not
 * a Collection. I realise this makes this name confusing so I apologise for
 * that.
 * </p>
 */
public interface BeanCollection<E> extends Serializable {
	
	/**
	 * Return true if this collection is owned by a sharedInstance.
	 * <p>
	 * That is, return true if it is a Many property of a bean in the cache.
	 * </p>
	 */
	public boolean isSharedInstance();
	
	/**
	 * Set when this collection is owned by a sharedInstance.
	 * <p>
	 * That is, it is a Many property on a bean in the cache.
	 * </p>
	 */
	public void setSharedInstance();
	
	/**
	 * Set a listener to be notified when the BeanCollection is first touched.
	 */
	public void setBeanCollectionTouched(BeanCollectionTouched notify);

	/**
	 * Re-attach the EbeanServer after deserialisation to allow lazy loading.
	 */
	public void setEbeanServer(LazyLoadEbeanServer ebeanServer);

	/**
	 * Set to true if you want the BeanCollection to be treated as read only.
	 * This means no elements can be added or removed etc.
	 */
	public void setReadOnly(boolean readOnly);
	
	/**
	 * Return true if the collection should be treated as readOnly and no
	 * elements can be added or removed etc.
	 */
	public boolean isReadOnly();
	
	/**
	 * Add the bean to the collection.
	 * <p>
	 * This is disallowed for BeanMap.
	 * </p>
	 */
	public void internalAdd(Object bean);
	
	/**
	 * Returns the underlying List Set or Map object.
	 */
	public Object getActualCollection();
	
	/**
	 * Return the number of elements in the List Set or Map.
	 */
	public int size();

	/**
	 * Return true if the List Set or Map is empty.
	 */
	public boolean isEmpty();
	
	/**
	 * Returns the underlying details as an iterator.
	 * <p>
	 * Note that for maps this returns the entrySet as we need the keys of the
	 * map.
	 * </p>
	 */
	public Iterator<E> getActualDetails();

	/**
	 * Set to true if maxRows was hit and there are actually more rows
	 * available.
	 * <p>
	 * Can be used by client code that is paging through results using
	 * setFirstRow() setMaxRows(). If this returns true then the client can
	 * display a 'next' button etc.
	 * </p>
	 */
	public boolean hasMoreRows();

	/**
	 * Set to true when maxRows is hit but there are actually more rows
	 * available. This is set so that client code knows that there is more data
	 * available.
	 */
	public void setHasMoreRows(boolean hasMoreRows);

	/**
	 * Returns true if the fetch has finished. False if the fetch is continuing
	 * in a background thread.
	 */
	public boolean isFinishedFetch();

	/**
	 * Set to true when a fetch has finished. Used when a fetch continues in the
	 * background.
	 */
	public void setFinishedFetch(boolean finishedFetch);

	/**
	 * return true if there are real rows held. Return false is this is using
	 * Deferred fetch to lazy load the rows and the rows have not yet been
	 * fetched.
	 */
	public boolean isPopulated();

	/**
	 * Set modify listening on or off. This is used to keep track of objects
	 * that have been added to or removed from the list set or map.
	 * <p>
	 * This is required only for ManyToMany collections. The additions and
	 * deletions are used to insert or delete entries from the intersection
	 * table. Otherwise modifyListening is false.
	 * </p>
	 */
	public void setModifyListening(boolean modifyListening);

	/**
	 * Add an object to the additions list.
	 * <p>
	 * This will potentially end up as an insert into a intersection table for a
	 * ManyToMany.
	 * </p>
	 */
	public void modifyAddition(E bean);

	/**
	 * Add an object to the deletions list.
	 * <p>
	 * This will potentially end up as an delete from an intersection table for
	 * a ManyToMany.
	 * </p>
	 */
	public void modifyRemoval(Object bean);

	/**
	 * Return the list of objects added to the list set or map. These will used
	 * to insert rows into the intersection table of a ManyToMany.
	 */
	public Set<E> getModifyAdditions();

	/**
	 * Return the list of objects removed from the list set or map. These will
	 * used to delete rows from the intersection table of a ManyToMany.
	 */
	public Set<E> getModifyRemovals();
	
	/**
	 * Reset the set of additions and deletions.
	 * This is called after the additions and removals have been processed.
	 */
	public void modifyReset();
}
