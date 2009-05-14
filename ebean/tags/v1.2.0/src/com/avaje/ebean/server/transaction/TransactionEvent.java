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
package com.avaje.ebean.server.transaction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.core.PersistRequest.Type;
import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Holds information for a transaction. There is one TransactionEvent instance
 * per Transaction instance.
 * <p>
 * When the associated Transaction commits or rollback this information is sent
 * to the TransactionEventManager.
 * </p>
 */
public class TransactionEvent implements Serializable {

	static final long serialVersionUID = 7230903304106097120L;

	/**
	 * Flag indicating this is a local transaction (not from another server in
	 * the cluster).
	 */
	transient boolean local;

	/**
	 * Lists of beans for BeanListener notification. Not sent across cluster in
	 * this form.
	 */
	transient TransactionEventBeans eventBeans;

	/**
	 * A Map of the TableModInfo objects. Keyed by tableName.
	 */
	HashMap<String, TableModInfo> tabModMap = new HashMap<String, TableModInfo>();

	boolean invalidateAll;

	/**
	 * Create the TransactionEvent, one per Transaction.
	 */
	public TransactionEvent() {
		this.local = true;
	}

	/**
	 * Set this to true to invalidate all table dependent cached objects.
	 */
	public void setInvalidateAll(boolean isInvalidateAll) {
		this.invalidateAll = isInvalidateAll;
	}

	/**
	 * Return true if all table states should be invalidated. This will cause
	 * all cached objects to be invalidated.
	 */
	public boolean isInvalidateAll() {
		return invalidateAll;
	}

	/**
	 * Return true if this was a local transaction. Returns false if this
	 * transaction originated on another server in the cluster.
	 */
	public boolean isLocal() {
		return local;
	}

	/**
	 * For BeanListeners the requests they are interested in.
	 */
	public TransactionEventBeans getEventBeans() {
		return eventBeans;
	}

	/**
	 * Returns true if this transaction contained modifications. If false then
	 * it could have been used for fetches only.
	 */
	public boolean hasModifications() {
		return invalidateAll || !tabModMap.isEmpty();
	}

	/**
	 * Add a inserted updated or deleted bean to the event.
	 * 
	 * @param request
	 *            the bean being persisted
	 * @param type
	 *            Insert, Update or Delete as per PersistType
	 */
	public void add(PersistRequestBean<?> request) {

		BeanDescriptor<?> desc = request.getBeanDescriptor();
		if (desc.getBeanListener() != null) {
			// a BeanListener is interested
			if (eventBeans == null) {
				eventBeans = new TransactionEventBeans();
			}
			eventBeans.add(request);
		}
		String table = desc.getBaseTable();
		Type type = request.getType();
		switch (type) {
		case INSERT:
			addInsert(table);
			break;

		case UPDATE:
			addUpdate(table);
			break;

		case DELETE:
			addDelete(table);
			break;

		default:
			break;
		}
	}

	/**
	 * Used when external code makes database modifications. These modifications
	 * need to be noted by ebean framework to invalidate the appropriate cached
	 * objects.
	 * <p>
	 * It doesn't really matter what the actual insert update or delete counts
	 * are. It only really matters if they are greater than 0.
	 * </p>
	 * 
	 * @param tableName
	 *            the table that was modified (case insensitive)
	 * @param inserts
	 *            the number of rows inserted
	 * @param updates
	 *            the number of rows updated
	 * @param deletes
	 *            the number of rows deleted
	 */
	public void add(String tableName, int inserts, int updates, int deletes) {

		if (inserts < 0 || updates < 0 || deletes < 0) {
			throw new PersistenceException("A negative row count was entered?");
		}

		TableModInfo tableMod = getTableModInfo(tableName);
		if (inserts > 0) {
			tableMod.incrementInsert(inserts);
		}
		if (updates > 0) {
			tableMod.incrementUpdate(updates);
		}
		if (updates > 0) {
			tableMod.incrementDelete(deletes);
		}
	}

	public void add(String tableName, boolean inserts, boolean updates, boolean deletes) {

		TableModInfo tableMod = getTableModInfo(tableName);
		if (inserts) {
			tableMod.incrementInsert(1);
		}
		if (updates) {
			tableMod.incrementUpdate(1);
		}
		if (deletes) {
			tableMod.incrementDelete(1);
		}
	}

	public void add(TransactionEvent event) {
		Iterator<TableModInfo> it = event.tableModInfoIterator();
		while (it.hasNext()) {
			TableModInfo info = (TableModInfo) it.next();
			add(info);
		}
	}

	protected void add(TableModInfo info) {

		TableModInfo modInfo = getTableModInfo(info.getTableName());
		modInfo.add(info);
	}

	/**
	 * Add a insert event for a table.
	 * 
	 * @param table
	 *            the name of the table inserted into
	 */
	public void addInsert(String table) {

		getTableModInfo(table).incrementInsert(1);
	}

	/**
	 * Add a update event for a table.
	 * 
	 * @param table
	 *            the name of the table updated
	 */
	public void addUpdate(String table) {

		getTableModInfo(table).incrementUpdate(1);
	}

	/**
	 * Add a delete event for a table.
	 * 
	 * @param table
	 *            the name of the table deleted from
	 */
	public void addDelete(String table) {

		getTableModInfo(table).incrementDelete(1);
	}

	private TableModInfo getTableModInfo(String tableName) {

		// uppercase to remove deployment case issues
		tableName = tableName.toUpperCase();

		TableModInfo modInfo = (TableModInfo) tabModMap.get(tableName);
		if (modInfo == null) {
			modInfo = new TableModInfo(tableName);
			tabModMap.put(tableName, modInfo);
		}
		return modInfo;
	}

	/**
	 * Return an Iterator of TableModInfo.
	 */
	public Iterator<TableModInfo> tableModInfoIterator() {
		return tabModMap.values().iterator();
	}

	public String toString() {
		return tabModMap.toString();
	}

}
