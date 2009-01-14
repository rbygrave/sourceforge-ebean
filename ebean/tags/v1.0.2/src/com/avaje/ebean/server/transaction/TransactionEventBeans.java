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

import java.util.ArrayList;

import com.avaje.ebean.server.core.PersistRequest;

/**
 * Lists of inserted updated and deleted beans that have a BeanListener.
 * <p>
 * These beans will be sent to the appropriate BeanListeners after a succcessful
 * commit of the transaction.
 * </p>
 */
public class TransactionEventBeans {

	ArrayList<PersistRequest> requests = new ArrayList<PersistRequest>();

	/**
	 * Return the list of PersistRequests that BeanListeners are interested in.
	 */
	public ArrayList<PersistRequest> getRequests() {
		return requests;
	}

	/**
	 * Add a bean for BeanListener notification.
	 */
	public void add(PersistRequest request) {

		requests.add(request);
	}

}
