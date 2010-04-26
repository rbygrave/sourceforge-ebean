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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.server.cluster.ClusterMessage;

public class McastListener implements Runnable {

    private static final Logger logger = Logger.getLogger(McastListener.class.getName());

    private final McastClusterBroadcast owner;
    
    private final MulticastSocket sock;

    private final Thread listenerThread;

    private final InetSocketAddress localSender;

    private final String localSenderIp;

    private final InetAddress group;

    private DatagramPacket pack;

    private byte[] receiveBuffer;

    private volatile boolean doingShutdown;

    private final boolean debugIgnore;
    
    public McastListener(McastClusterBroadcast owner, int port, String address, 
            int bufferSize, int timeout, InetSocketAddress localSender, 
            boolean disableLoopback, int ttl, InetAddress mcastBindAddress) {

        this.debugIgnore = GlobalProperties.getBoolean("ebean.debug.mcast.ignore", false);

        this.owner = owner;
        this.localSender = localSender;
        this.localSenderIp = localSender.getAddress().getHostAddress();

        this.receiveBuffer = new byte[bufferSize];
        this.listenerThread = new Thread(this, "EbeanClusterMcastListener");

        String msg = "Cluster Multicast Listen on["+address+":"+port+"] disableLoopback["+disableLoopback+"]";
        if (ttl >= 0){
            msg +=" ttl["+ttl+"]";
        }
        if (mcastBindAddress != null){
            msg += " mcastBindAddress["+mcastBindAddress+"]";
        }
        logger.info(msg);

        try {
            this.group = InetAddress.getByName(address);
            this.sock = new MulticastSocket(port);
            this.sock.setSoTimeout(timeout);

            if (disableLoopback){
                sock.setLoopbackMode(disableLoopback);
            }
            
            if (mcastBindAddress != null) {
                // bind to a specific interface
                sock.setInterface(mcastBindAddress);
            }
            
            if (ttl >= 0) {
                sock.setTimeToLive(ttl);
            }

            pack = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            sock.joinGroup(group);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startListening() {
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
    }

    /**
     * Shutdown this listener.
     */
    public void shutdown() {
        doingShutdown = true;
        listenerThread.interrupt();
        synchronized (listenerThread) {
            try {
                sock.leaveGroup(group);
            } catch (IOException e) {
                String msg = "Error leaving Multicast group";
                logger.log(Level.INFO, msg, e);
            }
            try {
                sock.close();
            } catch (Exception e) {
                String msg = "Error closing Multicast socket";
                logger.log(Level.INFO, msg, e);
            }
        }
    }

    public void run() {
        while (!doingShutdown) {
            try {
                synchronized (listenerThread) {
                    pack.setLength(receiveBuffer.length);
                    sock.receive(pack);

                    InetSocketAddress senderAddr = (InetSocketAddress)pack.getSocketAddress();
                    
                    String sa = senderAddr.getAddress().getHostAddress();
                    int ip = senderAddr.getPort();
                    
                    boolean sentByLocalSender = (localSender.getPort() == ip && localSenderIp.equals(sa));
                    
                    if (sentByLocalSender){
                        if (debugIgnore || logger.isLoggable(Level.FINE)){
                            logger.info("Ignoring message as sent by localSender: "+localSenderIp);
                        }
                    } else {
    
                        byte[] data = pack.getData();
                        ByteArrayInputStream bi = new ByteArrayInputStream(data);
                        ObjectInputStream ois = new ObjectInputStream(bi);
                        Object o = ois.readObject();
                        
                        if (o instanceof ClusterMessage == false){
                            String msg = "Error: Message object not a ClusterMessage? "+o.getClass().getName();
                            logger.log(Level.SEVERE, msg);
                            
                        } else {
                            
                            ClusterMessage cm = (ClusterMessage)o;
                            owner.handleMessage(cm);
                        }
                    }
                }
                
            } catch (java.net.SocketTimeoutException e) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "timeout", e);
                }
            } catch (IOException e) {
                logger.log(Level.INFO, "error ?", e);
                
            } catch (ClassNotFoundException e) {
                logger.log(Level.INFO, "error ?", e);
            }
        }
    }

}
