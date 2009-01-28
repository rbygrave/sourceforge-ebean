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
package org.avaje.ebean.server.lib.cluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.avaje.ebean.server.lib.ConfigProperties;
import org.avaje.ebean.server.net.ConnectionProcessor;
import org.avaje.ebean.server.net.Endpoint;
import org.avaje.ebean.server.net.Headers;
import org.avaje.ebean.server.net.SocketListener;
import org.avaje.lib.log.LogFactory;
import org.avaje.lib.util.StringHelper;

/**
 * Manages the cluster service.
 * <p>
 * SystemProperties:<br>
 * <pre><code>
 * ## the local member host:port
 * cluster.local=121.1.1.10:9001
 * 
 * ## all the members of the cluster
 * cluster.members=121.1.1.10:9001,121.1.1.10:9002,121.1.1.11:9001
 * 
 * ## specify the broadcast implementation, defaults to SocketBroadcast 
 * #cluster.broadcast=org.avaje.lib.cluster.SocketBroadcast
 * </code></pre>
 * </p>
 */
public class ClusterManager {

	private static final Logger logger = LogFactory.get(ClusterManager.class);
	
    SocketListener listener;
    
    Broadcast broadcast;
    
    Endpoint localMember = null;
    
    boolean isClusteringOn = false;

    ArrayList<Endpoint> otherMembers = new ArrayList<Endpoint>();
 
    public ClusterManager(ConfigProperties properties) {
     
        String localHostPort = properties.getProperty("cluster.local");
        if (localHostPort == null){
            // there is no clustering
            return;
        }
        try {
            
            String broadcastCn = properties.getProperty("cluster.broadcast");
            if (broadcastCn != null){
                Class<?> cls = Class.forName(broadcastCn);
                broadcast = (Broadcast)cls.newInstance();
            } else {
                broadcast = new SocketBroadcaster();
            }
            
            localMember = new Endpoint(localHostPort);
            
            String members = properties.getProperty("cluster.members");
            if (members != null){
                String[] memArray = StringHelper.delimitedToArray(members,",",false);
                for (int i = 0; i < memArray.length; i++) {
                    Endpoint member = new Endpoint(memArray[i]);
                    otherMembers.add(member);
                }
                otherMembers.remove(localMember);
            }
            
            listener = new SocketListener("ClusterListener", localMember.getPort());
            listener.startListening();
            
            // register broadcast to handle cluster register
            // and deregister messages
            registerProcessor("CLUSTER", broadcast);
            
            isClusteringOn = true;
            
            // tell the other cluster members that I have started
            Endpoint[] others = (Endpoint[])otherMembers.toArray(new Endpoint[otherMembers.size()]);
            broadcast.register(localMember, others);
            
        } catch (Exception ex){
            String msg = "Clustering has failed to start due to an exception.";
            logger.log(Level.SEVERE, msg, ex);
        }
    }
    
    
    /**
     * Register a processor for a given type of message.
     * @param key the processor id identifying the type of message
     * @param processor the processor that processes messages of this type
     */
    public void register(String key, ConnectionProcessor processor) {
        registerProcessor(key, processor);
    }

    private void registerProcessor(String key, ConnectionProcessor processor) {
        if (listener != null){
            listener.registerRequestHandler(key, processor);
        }
    }
    
    /**
     * Return true if clustering is on.
     */
    public boolean isClusteringOn() {
        return isClusteringOn;
    }
    
    /**
     * Send the message headers and payload to every server in the cluster.
     */
    public void broadcast(Headers headers, Serializable payload){
        broadcastMessage(headers, payload);
    }
    
    private void broadcastMessage(Headers headers, Serializable payload){
        if (broadcast != null){
            broadcast.broadcast(headers, payload);
        }
    }
    
    /**
     * Shutdown the service and Deregister from the cluster.
     */
    public void shutdown() {
        
        if (!isClusteringOn) {
            return;
        }
        logger.info("ClusterManager shutdown ");
        if (broadcast != null){
            // deregister from other cluster member...
            broadcast.deregister();
        }
        listener.shutdown();
    }
}
