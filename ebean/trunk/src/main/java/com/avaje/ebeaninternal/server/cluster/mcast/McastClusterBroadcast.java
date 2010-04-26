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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.cluster.ClusterBroadcast;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.cluster.ClusterMessage;

public class McastClusterBroadcast implements ClusterBroadcast {

    private ClusterManager clusterManager;
    
    private final McastListener listener;
    
    private final McastSender sender;
    
    public McastClusterBroadcast() {
        
        int port = GlobalProperties.getInt("ebean.cluster.mcast.listen.port", 0);
        String addr = GlobalProperties.get("ebean.cluster.mcast.listen.address", null);

        int sendPort = GlobalProperties.getInt("ebean.cluster.mcast.send.port", 0);
        String sendAddr = GlobalProperties.get("ebean.cluster.mcast.send.address", null);

        boolean disableLoopback = GlobalProperties.getBoolean("ebean.cluster.mcast.listen.disableLoopback", true);
        int ttl = GlobalProperties.getInt("ebean.cluster.mcast.listen.ttl", -1);
        String mcastAddr = GlobalProperties.get("ebean.cluster.mcast.listen.mcastAddress", null);
        
        InetAddress mcastAddress = null;
        if (mcastAddr != null){
            try {
                mcastAddress = InetAddress.getByName(mcastAddr);
            } catch (UnknownHostException e) {
                String msg = "Error getting Multicast InetAddress for " + mcastAddr;
                throw new RuntimeException(msg, e);
            }
        }
        
        if (port == 0 || addr == null){
            String msg = "One of these Multicast settings has not been set. "
                + "ebean.cluster.mcast.listen.port = "+port
                + ", ebean.cluster.mcast.listen.address="+addr;
                //+ ", ebean.cluster.mcast.sendport="+sendPort
                //+ ", ebean.cluster.mcast.sendaddress="+sendAddr;
                
            throw new IllegalArgumentException(msg);
        }
        
        int bufferSize = 65500;
        int timeout = 500;
        
        this.sender = new McastSender(port, addr, sendPort, sendAddr);
        this.listener = new McastListener(this, port, addr, bufferSize, timeout, sender.getAddress(), disableLoopback, ttl, mcastAddress);
    }
    
    public void broadcast(ClusterMessage message) {
        
        try {
            
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(ba);
            
            oos.writeObject(message);
            byte[] byteArray = ba.toByteArray();
            
            sender.sendMessage(byteArray);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        listener.shutdown();
    }

    public void startup(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        listener.startListening();
    }

    protected void handleMessage(ClusterMessage h){
        
        SpiEbeanServer server = (SpiEbeanServer)clusterManager.getServer(h.getEbeanServer());
        if (server != null){
            server.remoteTransactionEvent(h.getTransEvent());
        }

    }
    
    
}
