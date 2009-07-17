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
package com.avaje.ebean.server.persist;

import java.util.ArrayList;
import java.util.HashMap;

import com.avaje.ebean.internal.SpiTransaction;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.core.PersistRequestBean;

/**
 * Holds all the batched beans.
 * <p>
 * The beans are held here which delays the binding to a PreparedStatement. This
 * 'delayed' binding is required as the beans need to be bound and executed in
 * the correct order (according to the depth).
 * </p>
 */
public class BatchedBeanControl {

	/**
	 * Map of the BatchedBeanHolder objects. They each have a depth and are later
	 * sorted by their depth to get the execution order.
	 */
	private final HashMap<String, BatchedBeanHolder> beanHoldMap = new HashMap<String, BatchedBeanHolder>();

	private final SpiTransaction transaction;

	private final BatchControl batchControl;

	private int maxSize;

	public BatchedBeanControl(SpiTransaction t, BatchControl batchControl) {
		this.transaction = t;
		this.batchControl = batchControl;
	}

	/**
	 * Add the request to the batch.
	 */
	public void add(PersistRequestBean<?> request) {
		String typeDescription = request.getFullName();
		BatchedBeanHolder beanHolder = getBeanHolder(typeDescription);

		ArrayList<PersistRequest> list = beanHolder.getList(request.getType());
		list.add(request);

		// Maintain the max size across all the BeanHolders
		// This is used to determine when to flush the batch
		int bhSize = list.size();
		if (bhSize > maxSize) {
			maxSize = bhSize;
		}
	}

	/**
	 * Return an entry for the given type description. The type description is
	 * typically the bean class name (or table name for MapBeans).
	 */
	private BatchedBeanHolder getBeanHolder(String typeDesc) {
		BatchedBeanHolder list = (BatchedBeanHolder) beanHoldMap.get(typeDesc);
		if (list == null) {
			int depth = transaction.depth(0);
			list = new BatchedBeanHolder(batchControl, typeDesc, depth);
			beanHoldMap.put(typeDesc, list);
		}
		return list;
	}

	/**
	 * Return true if this holds no persist requests.
	 */
	public boolean isEmpty() {
		return beanHoldMap.isEmpty();
	}

	/**
	 * Return the held beans ready for sorting and executing.
	 * <p>
	 * This also has the effect of clearing the cache of beans held.
	 * </p>
	 */
	public BatchedBeanHolder[] getArray() {
		BatchedBeanHolder[] bsArray = new BatchedBeanHolder[beanHoldMap.size()];
		beanHoldMap.values().toArray(bsArray);
		beanHoldMap.clear();
		maxSize = 0;
		return bsArray;
	}

	/**
	 * Return the size of the biggest batch of beans (by type).
	 */
	public int getMaxSize() {
		return maxSize;
	}

}
