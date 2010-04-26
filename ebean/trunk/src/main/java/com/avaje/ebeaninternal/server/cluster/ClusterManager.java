/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebeaninternal.server.cluster;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.api.ClassUtil;
import com.avaje.ebeaninternal.server.cluster.mcast.McastClusterBroadcast;
import com.avaje.ebeaninternal.server.cluster.socket.SocketClusterBroadcast;

/**
 * Manages the cluster service.
 * <p>
 * ebean.properties:<br>
 * <pre class="code">
 * ## the local member host:port
 * cluster.local=121.1.1.10:9001
 * 
 * ## all the members of the cluster
 * cluster.members=121.1.1.10:9001,121.1.1.10:9002,121.1.1.11:9001
 * 
 * ## specify the broadcast implementation, defaults to SocketBroadcast 
 * #cluster.broadcast=com.avaje.lib.cluster.SocketBroadcast
 * </pre>
 * </p>
 */
public class ClusterManager {

	private static final Logger logger = Logger.getLogger(ClusterManager.class.getName());
        
    private final ClusterBroadcast broadcast;
    
    private final ConcurrentHashMap<String, EbeanServer> serverMap = new ConcurrentHashMap<String, EbeanServer>();

    private boolean started;
        
    public ClusterManager() {
        String local = GlobalProperties.get("ebean.cluster.local", null);
        String clusterType = GlobalProperties.get("ebean.cluster.type", "socket");
        
        if ("socket".equalsIgnoreCase(clusterType) && local == null){
            this.broadcast = null;            
            logger.info("Clustering not on");
            
        } else {
            try {
                if ("mcast".equalsIgnoreCase(clusterType)) {
                    this.broadcast = new McastClusterBroadcast();
                    
                } else if ("socket".equalsIgnoreCase(clusterType)) {
                    this.broadcast = new SocketClusterBroadcast();
                    
                } else {
                    this.broadcast = (ClusterBroadcast)ClassUtil.newInstance(clusterType);
                }
                
                logger.info("Clustering on using ["+broadcast.getClass().getName()+"]");
                
            } catch (Exception e){
                String msg = "Error initialising ClusterManager type ["+clusterType+"]";
                logger.log(Level.SEVERE, msg, e);
                
                throw new RuntimeException(e);
            }
        }
    }
    
    public void registerServer(EbeanServer server){
        if (!started){
            startup();
        }
        serverMap.put(server.getName(), server);
    }
    
    public EbeanServer getServer(String name){
        return serverMap.get(name);
    }

    private void startup() {
        started = true;
        if (broadcast != null){
            broadcast.startup(this);
        }
    }
    
    /**
     * Return true if clustering is on.
     */
    public boolean isClustering() {
        return broadcast != null;
    }
    
    /**
     * Send the message headers and payload to every server in the cluster.
     */
    public void broadcast(ClusterMessage headers){
        if (broadcast != null){
            broadcast.broadcast(headers);
        }
    }
    
    /**
     * Shutdown the service and Deregister from the cluster.
     */
    public void shutdown() {
        
        if (broadcast != null) {
            logger.info("ClusterManager shutdown ");
            broadcast.shutdown();
        }
    }
}
