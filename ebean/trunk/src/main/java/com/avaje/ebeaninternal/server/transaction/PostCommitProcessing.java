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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebeaninternal.api.TransactionEvent;
import com.avaje.ebeaninternal.api.TransactionEventBeans;
import com.avaje.ebeaninternal.api.TransactionEventTable;
import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.core.PersistRequestBean;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptorManager;

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
	
	private final List<PersistRequestBean<?>> persistBeanRequests;
	
	private final BeanPersistIdMap beanPersistIdMap;
	
	private final BeanDeltaMap beanDeltaMap;
	
	/**
	 * Create for a TransactionManager and event.
	 */
	public PostCommitProcessing(ClusterManager clusterManager, TransactionManager manager, TransactionEvent event) {
		this.clusterManager = clusterManager;
		this.manager = manager;
		this.serverName = manager.getServerName();
		this.event = event;
		this.persistBeanRequests = getPersistBeanRequests();
		this.beanPersistIdMap = createBeanPersistIdMap();
		this.beanDeltaMap = new BeanDeltaMap(event.getBeanDeltas());
	}

	private List<PersistRequestBean<?>> getPersistBeanRequests() {
	    TransactionEventBeans eventBeans = event.getEventBeans();
        if (eventBeans != null){
            return eventBeans.getRequests();
        }
        return null;
	}
	
	private BeanPersistIdMap createBeanPersistIdMap() {
	    BeanPersistIdMap m = new BeanPersistIdMap();
        if (persistBeanRequests != null){
            for (int i = 0; i < persistBeanRequests.size(); i++) {
                // notify local BeanPersistListener's and at the 
                // request IUD type and id to the RemoteTransactionEvent
                persistBeanRequests.get(i).addToPersistMap(m);
            }
        }
        return m;
	}
	
	/**
	 * Return a the Id's of the beans persisted organised by their type.
	 */
	public BeanPersistIdMap getBeanPersistIdMap() {
        return beanPersistIdMap;
    }
	
	public void localBeanDeltaNotify() {
	    beanDeltaMap.process();
	}

	public void localCacheNotify() {
        // notify cache with bean changes
        event.notifyCache();
        TransactionEventTable tableEvents = event.getEventTables();
        processTableEvents(tableEvents);
    }
	
	   /**
     * Table events are where SQL or external tools are used. In this case
     * the cache is notified based on the table name (rather than bean type).
     */
    private void processTableEvents(TransactionEventTable tableEvents) {
        
        if (tableEvents != null && !tableEvents.isEmpty()){
            // notify cache with table based changes
            BeanDescriptorManager dm = manager.getBeanDescriptorManager();
            for (TableIUD tableIUD : tableEvents.values()) {
                dm.cacheNotify(tableIUD);
            }
        }
    }

    /**
	 * Run the processing.
	 */
	public void run() {

	    localPersistListenersNotify();
		
	    if (clusterManager.isClustering()){
	        
    		RemoteTransactionEvent remoteEvent = new RemoteTransactionEvent(serverName);
    		
    		//TODO: Should only transport BeanDelta's to Index Writers 
            for (BeanDeltaList deltaList: beanDeltaMap.deltaLists()) {
                remoteEvent.addBeanDeltaList(deltaList);
            }

            for (BeanPersistIds beanPersist : beanPersistIdMap.values()) {
                remoteEvent.addBeanPersistIds(beanPersist);
            }
    		
    		TransactionEventTable eventTables = event.getEventTables();
    		if (eventTables != null && !eventTables.isEmpty()){
    		    for (TableIUD tableIUD : eventTables.values()) {
    		        remoteEvent.addTableIUD(tableIUD);
                }		    
    		}
    
    		if (!remoteEvent.isEmpty()) {
    			// send the interesting events to the cluster
                if (manager.getClusterDebugLevel() > 0 || logger.isLoggable(Level.FINE)) {
                    logger.info("Cluster Send: " + remoteEvent.toString());
                }
    
                clusterManager.broadcast(remoteEvent);
    		}
	    }
	}
	
    private void localPersistListenersNotify() {
        if (persistBeanRequests != null) {
            for (int i = 0; i < persistBeanRequests.size(); i++) {
                persistBeanRequests.get(i).notifyLocalPersistListener();
            }
        }
    }


}
