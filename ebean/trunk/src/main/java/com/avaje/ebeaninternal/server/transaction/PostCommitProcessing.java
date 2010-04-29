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
package com.avaje.ebeaninternal.server.transaction;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebeaninternal.api.TransactionEvent;
import com.avaje.ebeaninternal.api.TransactionEventBeans;
import com.avaje.ebeaninternal.api.TransactionEventTable;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.core.PersistRequestBean;

/**
 * Performs post commit processing using a background thread.
 * <p>
 * This includes Cluster notification, and BeanPersistListeners.
 * </p>
 */
public final class PostCommitProcessing implements Runnable {
    
    private static final Logger logger = Logger.getLogger(PostCommitProcessing.class.getName());
    
	private final ClusterManager clusterManager;
	
	private final TransactionEvent event;

	private final String serverName;
	
	private final TransactionManager manager;
	
	/**
	 * Create for a TransactionManager and event.
	 */
	public PostCommitProcessing(ClusterManager clusterManager, TransactionManager manager, TransactionEvent event) {
		this.clusterManager = clusterManager;
		this.manager = manager;
		this.serverName = manager.getServerName();
		this.event = event;
	}

	/**
	 * Run the processing.
	 */
	public void run() {

		RemoteTransactionEvent remoteEvent = new RemoteTransactionEvent(serverName);

		TransactionEventBeans eventBeans = event.getEventBeans();
		if (eventBeans != null){
			ArrayList<PersistRequestBean<?>> requests = eventBeans.getRequests();
			if (requests != null){
				for (int i = 0; i < requests.size(); i++) {
			        // notify local BeanPersistListener's and at the 
			        // request IUD type and id to the RemoteTransactionEvent
					requests.get(i).notifyLocalPersistListener(remoteEvent);
				}
			}
		}
		
		TransactionEventTable eventTables = event.getEventTables();
		if (eventTables != null && !eventTables.isEmpty()){
			remoteEvent.setTableEvents(event.getEventTables());
		}

		if (remoteEvent.hasEvents() && clusterManager.isClustering()) {
			// send the interesting events to the cluster
            if (manager.isDebugRemote() || logger.isLoggable(Level.FINE)) {
                logger.info("Cluster Send: " + remoteEvent.toString());
            }

            clusterManager.broadcast(remoteEvent);
		}
	}
	
}
