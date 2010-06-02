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
package com.avaje.ebeaninternal.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.server.core.PersistRequestBean;
import com.avaje.ebeaninternal.server.transaction.BeanDelta;

/**
 * Holds information for a transaction. There is one TransactionEvent instance
 * per Transaction instance.
 * <p>
 * When the associated Transaction commits or rollback this information is sent
 * to the TransactionEventManager.
 * </p>
 */
public class TransactionEvent implements Serializable {

	private static final long serialVersionUID = 7230903304106097120L;

	/**
	 * Flag indicating this is a local transaction (not from another server in
	 * the cluster).
	 */
	private transient boolean local;

	/**
	 * Lists of beans for BeanListener notification. Not sent across cluster in
	 * this form.
	 */
	private transient TransactionEventBeans eventBeans;

    private transient List<BeanDelta> beanDeltas;

	private TransactionEventTable eventTables;
	
	private boolean invalidateAll;
	
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

    public void addBeanDelta(BeanDelta delta) {
        if (beanDeltas == null) {
            beanDeltas = new ArrayList<BeanDelta>();
        }
        beanDeltas.add(delta);
    }

    public List<BeanDelta> getBeanDeltas() {
        return beanDeltas;
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

	public TransactionEventTable getEventTables() {
		return eventTables;
	}

	public void add(String tableName, boolean inserts, boolean updates, boolean deletes){
		if (eventTables == null){
			eventTables = new TransactionEventTable();
		}
		eventTables.add(tableName, inserts, updates, deletes);		
	}
	
	public void add(TransactionEventTable table){
		if (eventTables == null){
			eventTables = new TransactionEventTable();
		}
		eventTables.add(table);
	}
	
	/**
	 * Add a inserted updated or deleted bean to the event.
	 */
	public void add(PersistRequestBean<?> request) {

		if (request.isNotify()){
			// either a BeanListener or Cache is interested
			if (eventBeans == null) {
				eventBeans = new TransactionEventBeans();
			}
			eventBeans.add(request);
		}
	}

	/**
	 * Notify the cache of bean changes.
	 * <p>
	 * This returns the TransactionEventTable so that if any 
	 * general table changes can also be used to invalidate 
	 * parts of the cache.
	 * </p>
	 */
	public void notifyCache(){
		if (eventBeans != null){
			eventBeans.notifyCache();
		}
	}

}
