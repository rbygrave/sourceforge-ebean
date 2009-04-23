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

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Holds sets of additions and deletions from a 'owner' List Set or Map.
 * <p>
 * These sets of additions and deletions are used to support persisting
 * ManyToMany relationships. The additions becoming inserts into the
 * intersection table and the removals becoming deletes from the intersection
 * table.
 * </p>
 */
class ModifyHolder<E> implements Serializable {

	private static final long serialVersionUID = 2572572897923801083L;

	/**
	 * Deletions list for manyToMany persistence.
	 */
	Set<E> modifyDeletions = new LinkedHashSet<E>();

	/**
	 * Additions list for manyToMany persistence.
	 */
	Set<E> modifyAdditions = new LinkedHashSet<E>();

	void reset() {
		modifyDeletions = new LinkedHashSet<E>();
		modifyAdditions = new LinkedHashSet<E>();		
	}
	
	/**
	 * Used by BeanList.addAll() methods.
	 */
	void modifyAdditionAll(Collection<? extends E> c) {
		if (c != null) {
			modifyAdditions.addAll(c);
		}
	}

	void modifyAddition(E bean) {
		if (bean != null) {
			modifyAdditions.add(bean);
		}
	}

	@SuppressWarnings("unchecked")
	void modifyRemoval(Object bean) {
		if (bean != null) {
			modifyDeletions.add((E)bean);
		}
	}

	Set<E> getModifyAdditions() {
		return modifyAdditions;
	}

	Set<E> getModifyRemovals() {
		return modifyDeletions;
	}
}
