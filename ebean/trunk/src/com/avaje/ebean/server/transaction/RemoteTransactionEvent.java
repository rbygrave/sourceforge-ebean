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
import java.util.ArrayList;
import java.util.List;

/**
 * Holds information for a transaction that is sent around the cluster.
 * <p>
 * Holds a collection of RemoteBeanPersist objects used to notify
 * BeanPersistListeners and potentially a TransactionEventTable.
 * </p>
 */
public final class RemoteTransactionEvent implements Serializable {

	private static final long serialVersionUID = 5790053761599631177L;

	private ArrayList<RemoteBeanPersist> list;

	private TransactionEventTable tableEvents;

	public RemoteTransactionEvent() {

	}

	/**
	 * Return true if this has some bean persist or table events.
	 */
	public boolean hasEvents() {
		return (list != null && !list.isEmpty()) || tableEvents != null;
	}

	/**
	 * Set the table events.
	 */
	public void setTableEvents(TransactionEventTable tableEvents) {
		this.tableEvents = tableEvents;
	}

	/**
	 * Return the table events if there where any.
	 */
	public TransactionEventTable getTableEvents() {
		return tableEvents;
	}

	/**
	 * Add a Insert Update or Delete payload.
	 */
	public void add(RemoteBeanPersist payload) {
		if (list == null){
			list = new ArrayList<RemoteBeanPersist>();
		}
		list.add(payload);
	}

	/**
	 * Return the list of RemoteListenerPayload.
	 */
	public List<RemoteBeanPersist> getBeanPersistList() {
		return list;
	}
}
