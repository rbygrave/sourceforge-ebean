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

import java.io.ObjectStreamException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.avaje.ebean.common.InternalEbean;
import com.avaje.ebean.common.ObjectGraphNode;
import com.avaje.ebean.io.SerializeControl;

/**
 * Set capable of lazy loading.
 */
public final class BeanSet<E> implements Set<E>, BeanCollection<E> {
    
	private static final long serialVersionUID = 2435724099234280064L;
    
	/**
	 * The EbeanServer this is associated with. (used for lazy fetch).
	 */
	private transient InternalEbean internalEbean;
    
	private transient final ObjectGraphNode profilePoint;
	
	/**
	 * The owning bean (used for lazy fetch).
	 */
	private final Object ownerBean;

	/**
	 * The name of this property in the owning bean (used for lazy fetch).
	 */
	private final String propertyName;
	
    /**
     * The underlying Set implementation.
     */
	private Set<E> set;

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
     * Create with a specific Set implementation.
     */
    public BeanSet(Set<E> set) {
        this.set = set;
        this.profilePoint = null;
        this.propertyName = null;
        this.ownerBean = null;
    }
    
    /**
     * Create using an underlying LinkedHashSet.
     */
    public BeanSet() {
        this(new LinkedHashSet<E>());
    }
	
    public BeanSet(InternalEbean internalEbean, Object ownerBean, String propertyName, ObjectGraphNode profilePoint) {
		this.internalEbean = internalEbean;
		this.ownerBean = ownerBean;
		this.propertyName = propertyName;
		this.profilePoint = profilePoint;
	}
	
    Object readResolve() throws ObjectStreamException {
        if (SerializeControl.isVanillaCollections()){
            return set;
        }
        return this;
    }
    
    Object writeReplace() throws ObjectStreamException {
        if (SerializeControl.isVanillaCollections()){
            return set;
        }
        return this;
    }
    
    @SuppressWarnings("unchecked")
	public void internalAdd(Object bean) {
		set.add((E)bean);
	}
	
    /**
     * Returns true if the underlying set has its data.
     */
    public boolean isPopulated() {
        return set != null;
    }

	private void init() {
        if (set == null && internalEbean != null) {
			//InternalEbean eb = (InternalEbean)Ebean.getServer(serverName);
			internalEbean.lazyLoadMany(ownerBean, propertyName, profilePoint);
        }
    }

    /**
     * Set the underlying set (used for lazy fetch).
     */
    @SuppressWarnings("unchecked")
	public void setActualSet(Set<?> set){
    	this.set = (Set<E>)set;
    }
    
	/**
     * Return the actual underlying set.
     */
    public Set<E> getActualSet() {
        return set;
    }
    
	public Iterator<?> getActualDetails() {
		return set.iterator();
	}
	
	/**
	 * Returns the underlying set.
	 */
	public Object getActualCollection(){
		return set;
	}
	

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("BeanSet ");
        if (set == null) {
            sb.append("deferred ");
            
        } else {
            sb.append("size[").append(set.size()).append("]");
            sb.append(" hasMoreRows[").append(hasMoreRows).append("]");
            sb.append(" set").append(set);
        }
        return sb.toString();
    }

    /**
     * Equal if obj is a Set and equal in a Set sense.
     */
    public boolean equals(Object obj) {
        init();
        return set.equals(obj);
    }

    public int hashCode() {
        init();
        return set.hashCode();
    }
    

    //  -----------------------------------------------------//
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
    
    public boolean add(E o) {
        init();
        if (modifyListening){
        	if (set.add(o)){
        		modifyAddition(o);
        		return true;
        	} else {
        		return false;
        	}
        }
        return set.add(o);
    }

    public boolean addAll(Collection<? extends E> c) {
        init();
        if (modifyListening){
        	boolean changed = false;
        	Iterator<? extends E> it = c.iterator();
        	while (it.hasNext()) {
				E o = it.next();
				if (set.add(o)){
					modifyAddition(o);
					changed = true;
				}
			}
        	return changed;
        }
        return set.addAll(c);
    }

    public void clear() {
        init();
        set.clear();
    }

    public boolean contains(Object o) {
        init();
        return set.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        init();
        return set.containsAll(c);
    }

    public boolean isEmpty() {
        init();
        return set.isEmpty();
    }

    public Iterator<E> iterator() {
        init();
        if (modifyListening){
        	return new ModifyIterator<E>(this, set.iterator());
        }
        return set.iterator();
    }

    public boolean remove(Object o) {
        init();
        if (modifyListening){
        	if (set.remove(o)){
        		modifyRemoval(o);
        	}
        }
        return set.remove(o);
    }

    public boolean removeAll(Collection<?> c) {
        init();
        if (modifyListening){
        	boolean changed = false;
        	Iterator<?> it = c.iterator();
        	while (it.hasNext()) {
				Object o = (Object) it.next();
				if (set.remove(o)){
					modifyRemoval(o);
					changed = true;
				}
			}
        	return changed;
        }
        return set.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        init();
        if (modifyListening){
        	boolean changed = false;
        	Iterator<?> it = set.iterator();
        	while (it.hasNext()) {
				Object o = it.next();
				if (!c.contains(o)){
					it.remove();
					modifyRemoval(o);
					changed = true;
				}
			}
        	return changed;
        }
        return set.retainAll(c);
    }

    public int size() {
        init();
        return set.size();
    }

    public Object[] toArray() {
        init();
        return set.toArray();
    }

    public <T> T[] toArray(T[] a) {
        init();
        return set.toArray(a);
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
