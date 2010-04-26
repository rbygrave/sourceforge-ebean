/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebeaninternal.server.cluster.mcast;

import java.io.IOException;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.server.cluster.ClusterBroadcast;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.cluster.ClusterMessage;

public class McastClusterBroadcast implements ClusterBroadcast {

    //private ClusterManager clusterManager;
    
    private final McastListener listener;
    
    private final McastSender sender;
    
    public McastClusterBroadcast() {
        
        int port = GlobalProperties.getInt("ebean.cluster.mcast.port", 7768);
        String addr = GlobalProperties.get("ebean.cluster.mcast.address", "235.1.1.1");

        int sendPort = GlobalProperties.getInt("ebean.cluster.mcast.sendport", 9768);
        String sendAddr = GlobalProperties.get("ebean.cluster.mcast.sendaddress", null);

        sender = new McastSender(port, addr, sendPort, sendAddr);
        listener = new McastListener(port, addr, 30000, 500, sender.getAddress());
    }
    
    public void broadcast(ClusterMessage message) {
        String s = message.toString();
        try {
            sender.sendMessage(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        listener.shutdown();
    }

    public void startup(ClusterManager clusterManager) {
        //this.clusterManager = clusterManager;
        listener.startListening();
    }

    
}
