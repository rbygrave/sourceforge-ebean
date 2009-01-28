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
package org.avaje.ebean.server.transaction;

/**
 * Performs post commit processing (using a background thread).
 * <p>
 * This includes Cluster notification, Lucene notification and BeanListener
 * notification.
 * </p>
 */
public class PostCommitNotify implements Runnable {

	private final TransactionManager manager;

	private final TransactionEvent event;

	/**
	 * Create for a TransactionManager and event.
	 */
	public PostCommitNotify(TransactionManager manager, TransactionEvent event) {
		this.manager = manager;
		this.event = event;
	}

	/**
	 * Run the processing.
	 */
	public void run() {
		manager.notifyCluster(event);
		//manager.notifyLucene(event);
		manager.notifyBeanListeners(event);
	}

}
