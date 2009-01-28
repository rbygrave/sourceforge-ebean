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
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.avaje.ebean.server.net.Endpoint;
import org.avaje.ebean.server.net.Headers;
import org.avaje.ebean.server.net.IoConnection;
import org.avaje.ebean.server.net.SocketClient;
import org.avaje.lib.log.LogFactory;

/**
 * Broadcast messages across the cluster using sockets. 
 */
public class SocketBroadcaster extends Broadcast {

	private static final Logger logger = LogFactory.get(SocketBroadcaster.class);
	
    boolean sendSecure = false;
    
    Endpoint local;
    
    HashMap<String,SocketClient> clientMap = new HashMap<String, SocketClient>();
    
    SocketClient[] members;

    /**
     * Register with all the other members of the Cluster.
     */
    public void register(Endpoint local, Endpoint[] others) {
    	
        this.local = local;
        
        Headers h = new Headers();
        h.setProcesorId("CLUSTER");
        h.set("REGISTER", local.getFullName());
        
        for (int i = 0; i < others.length; i++) {
		
            Endpoint member = others[i];
            SocketClient sc = create(member);
            
            boolean isOnline = send(sc, h, null, false);
            sc.setOnline(isOnline);
            
            String msg = "Cluster Member ["+member.getFullName()+"] isOnline["+isOnline+"]";
            logger.info(msg);
        }
    }

    private SocketClient create(Endpoint endpoint) {
    	
        SocketClient client = new SocketClient();
        client.setEndpoint(endpoint);
        clientMap.put(endpoint.getFullName(), client);
        members = null;
        return client;
    }

    private void setMemberOnline(String fullName, boolean isOnline){
        synchronized (clientMap) {
            String msg = "Cluster Member ["+fullName+"] isOnline["+isOnline+"]";
            logger.info(msg);
            SocketClient member = clientMap.get(fullName);
            member.setOnline(isOnline);
        }
    }

    private boolean send(SocketClient client, Headers headers, Serializable payload, boolean logError) {

        try {        
            IoConnection sc = client.createConnection(sendSecure);
            sc.writeObject(headers);
            if (payload != null){
                sc.writeObject(payload);
            }
            sc.getObjectOutputStream().flush();
            sc.disconnect();
            return true;
            
        } catch (Exception ex){
            if (logError){
            	logger.log(Level.SEVERE, "Error sending message", ex);
            }
            return false;
        }
    }
    
    
    private SocketClient[] getMembers(){
    	if (members == null){
    		Collection<SocketClient> c = clientMap.values();
    		members = c.toArray(new SocketClient[c.size()]);
    	}
    	return members;
    }
    
    /**
     * Send the payload to all the members of the cluster.
     */
    public boolean broadcast(Headers headers, Serializable payload) {
    	
        boolean errors = false;
        
        SocketClient[] members = getMembers();
        
        for (int i = 0; i < members.length; i++) {
            if (members[i].isOnline()) {
                if (!send(members[i], headers, payload, true)) {
                    errors = true;
                }
            }
        }
        return errors;
    }

    /**
     * Deregister from the cluster.
     */
    public void deregister() {
        Headers h = new Headers();
        h.setProcesorId("CLUSTER");
        h.set("DEREGISTER", local.getFullName());
        broadcast(h, null);
    }

    /**
     * Process a Member Registration or Deregistration.
     */
    public void process(IoConnection request) {
    	
        Headers h = request.getHeaders();
        
        String regHost = h.get("REGISTER");
        if (regHost != null){
            setMemberOnline(regHost, true);
        }
        regHost = h.get("DEREGISTER");
        if (regHost != null){
            setMemberOnline(regHost, false);
        }
        throw new RuntimeException("Unhandled message type ["+h+"]");
    }
    
}
