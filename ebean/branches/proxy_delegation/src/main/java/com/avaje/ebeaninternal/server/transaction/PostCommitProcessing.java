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

import com.avaje.ebeaninternal.api.TransactionEvent;
import com.avaje.ebeaninternal.api.TransactionEventBeans;
import com.avaje.ebeaninternal.api.TransactionEventTable;
import com.avaje.ebeaninternal.net.Constants;
import com.avaje.ebeaninternal.server.core.PersistRequestBean;
import com.avaje.ebeaninternal.server.lib.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.net.CmdRemoteTransactionEvent;
import com.avaje.ebeaninternal.server.net.Headers;

/**
 * Performs post commit processing using a background thread.
 * <p>
 * This includes Cluster notification, and BeanPersistListeners.
 * </p>
 */
public final class PostCommitProcessing implements Runnable, Constants {

	private final ClusterManager clusterManager;
	
	private final TransactionEvent event;

	private final String serverName;
	
	/**
	 * Create for a TransactionManager and event.
	 */
	public PostCommitProcessing(ClusterManager clusterManager, TransactionManager manager, TransactionEvent event) {
		this.clusterManager = clusterManager;
		this.serverName = manager.getServerName();
		this.event = event;
	}

	/**
	 * Run the processing.
	 */
	public void run() {

		RemoteTransactionEvent remoteEvent = new RemoteTransactionEvent();

		// notify local BeanPersistListener's
		TransactionEventBeans eventBeans = event.getEventBeans();
		if (eventBeans != null){
			ArrayList<PersistRequestBean<?>> requests = eventBeans.getRequests();
			if (requests != null){
				for (int i = 0; i < requests.size(); i++) {
					RemoteBeanPersist remote  = requests.get(i).notifyLocalPersistListener();
					if (remote != null){
						// need to send this event across the cluster
						remoteEvent.add(remote);
					}
				}
			}
		}
		TransactionEventTable eventTables = event.getEventTables();
		if (eventTables != null && !eventTables.isEmpty()){
			remoteEvent.setTableEvents(event.getEventTables());
		}

		if (remoteEvent.hasEvents()) {
			// send the interesting events to the cluster
			sendEventToCluster(remoteEvent);
		}
	}
	

	/**
	 * Broadcast TransactionEvent to the other servers in the cluster.
	 */
	private void sendEventToCluster(RemoteTransactionEvent e) {

		if (clusterManager.isClusteringOn()) {
			Headers h = new Headers();
			h.setProcesorId(PROCESS_KEY);
			h.set(SERVER_NAME_KEY, serverName);

			CmdRemoteTransactionEvent cmd = new CmdRemoteTransactionEvent(e);
			clusterManager.broadcast(h, cmd);
		}
	}
}
