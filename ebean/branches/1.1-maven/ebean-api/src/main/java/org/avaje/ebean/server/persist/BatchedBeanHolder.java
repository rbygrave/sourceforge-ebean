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
package org.avaje.ebean.server.persist;

import java.util.ArrayList;

import org.avaje.ebean.server.core.PersistRequest;

/**
 * Holds lists of persist requests for beans of a given typeDescription.
 * <p>
 * This is used to delay the actual binding of the bean to PreparedStatements.
 * The reason is that don't have all the bind values yet in the case of inserts
 * with getGeneratedKeys.
 * </p>
 * <p>
 * Has a depth which is used to determine the order in which it should be
 * executed. The lowest depth is executed first.
 * </p>
 */
public class BatchedBeanHolder {

	/**
	 * The owning queue.
	 */
	private final BatchControl control;

	/**
	 * The type of all the requests.
	 */
	private final String typeDesc;

	/**
	 * The 'depth' which is used to determine the execution order.
	 */
	private final int depth;

	/**
	 * The list of bean insert requests.
	 */
	ArrayList<PersistRequest> inserts;

	/**
	 * The list of bean update requests.
	 */
	ArrayList<PersistRequest> updates;

	/**
	 * The list of bean delete requests.
	 */
	ArrayList<PersistRequest> deletes;

	/**
	 * Create a new entry with a given type and depth.
	 */
	public BatchedBeanHolder(BatchControl control, String typeDesc, int depth) {
		this.control = control;
		this.typeDesc = typeDesc;
		this.depth = depth;
	}

	/**
	 * Return the depth.
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Return the type description.
	 * <p>
	 * This is the for Beans the bean class name, for MapBeans the baseTable,
	 * for updateSql the class name and label, for callableSql the class name
	 * and label.
	 * </p>
	 */
	public String getTypeDescription() {
		return typeDesc;
	}

//	/**
//	 * Add the request to the appropriate list returning the size of the list.
//	 */
//	public boolean queue(PersistRequest request, int batchSize) {
//		ArrayList<PersistRequest> list = getList(request.getType());
//		if (list.size() >= batchSize){
//			return false;
//		} else {
//			list.add(request);
//			return true;
//		}
//	}
	
	/**
	 * Execute all the persist requests in this entry.
	 * <p>
	 * This will Batch all the similar requests into one or more BatchStatements
	 * and then execute them.
	 * </p>
	 */
	public void executeNow() {
		// process the requests. Creates one or more PreparedStatements
		// with binding addBatch() for each request.

		// Note updates and deletes can result in many PreparedStatements
		// if their where clauses differ via use of IS NOT NULL.
		if (inserts != null) {
			control.executeNow(inserts);
			inserts.clear();
		}
		if (updates != null) {
			control.executeNow(updates);
			updates.clear();
		}
		if (deletes != null) {
			control.executeNow(deletes);
			deletes.clear();
		}
	}

	public String toString() {
		return typeDesc + " depth:" + depth;
	}

	/**
	 * Return the list for the typeCode.
	 */
	protected ArrayList<PersistRequest> getList(PersistRequest.Type type) {
		switch (type) {
		case INSERT:
			if (inserts == null) {
				inserts = new ArrayList<PersistRequest>();
			}
			return inserts;

		case UPDATE:
			if (updates == null) {
				updates = new ArrayList<PersistRequest>();
			}
			return updates;

		case DELETE:
			if (deletes == null) {
				deletes = new ArrayList<PersistRequest>();
			}
			return deletes;

		default:
			throw new RuntimeException("Invalid type code " + type);
		}
	}
}
