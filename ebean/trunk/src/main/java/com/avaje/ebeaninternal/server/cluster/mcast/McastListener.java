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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

public class McastListener implements Runnable {

    private static final Logger logger = Logger.getLogger(McastListener.class.getName());

    public final int port;// = 7778;

    public final String address;// = "235.1.1.1";

    public final int bufferSize;// = 200;

    private final MulticastSocket sock;

    private DatagramPacket pack;

    private byte[] receiveBuffer;

    private final Thread listenerThread;
    
    volatile boolean doingShutdown;

    private final InetSocketAddress localSender;
    private final String localSenderIp;
    
    private final InetAddress group;
    
    public McastListener(int port, String address, int bufferSize, int timeout, InetSocketAddress localSender) {

        this.port = port;
        this.address = address;
        this.bufferSize = bufferSize;
        this.localSender = localSender;
        this.localSenderIp = localSender.getAddress().getHostAddress();
        
        this.receiveBuffer = new byte[bufferSize];
        this.listenerThread = new Thread(this, "ebean.cluster.McastListener");

        try {
            this.group = InetAddress.getByName(address);
            this.sock = new MulticastSocket(port);
            this.sock.setSoTimeout(timeout);
            
//            boolean localLoopbackDisabled = false;            
//            sock.setLoopbackMode(localLoopbackDisabled);
            
            // multihomed?
//            if (mcastBindAddress != null){
//                sock.setInterface(mcastBindAddress);
//            }            
//            if ( 0 >= 0 ) {
//                sock.setTimeToLive(20);
//            }

            
            
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
        System.out.println("-- shutdown");
        listenerThread.interrupt();
        System.out.println("-- interrupted");
        synchronized (listenerThread) {    
            try {
                System.out.println("-- leave");
                sock.leaveGroup(group);
            } catch (IOException e) {
                String msg = "Error leaving Multicast group";
                logger.log(Level.INFO, msg, e);
            }
            try {
                System.out.println("-- close");
                sock.close();
            } catch ( Exception e){
                String msg = "Error closing Multicast socket";
                logger.log(Level.INFO, msg, e);
            }
            System.out.println("-- done");
        }
    }
        
    public void run() {
        while (!doingShutdown) {
            try {
                synchronized (listenerThread) {
                    pack.setLength(receiveBuffer.length);
                    sock.receive(pack);
                    
                    //if (localSenderIp != null) {
                        SocketAddress socketAddress = pack.getSocketAddress();
                        if (socketAddress instanceof InetSocketAddress){
                            InetSocketAddress inet = (InetSocketAddress)socketAddress;
                            System.out.println("recieved 1... "+inet);
                            String sa = inet.getAddress().getHostAddress();
                            int ip = inet.getPort();
                            System.out.println("recieved 2... "+ip+" "+sa);
                            System.out.println("LOCAL "+localSender.getPort()+" "+localSenderIp);
                            if (localSender.getPort() == ip && localSenderIp.equals(sa)) {
                                System.out.println(" ----------------- SAME ADDRESS "+inet);
                            }
                        } else {
                            System.out.println("Hum? "+socketAddress.getClass().getName());
                        }
                    //}
                    
                    InetAddress address2 = pack.getAddress();
                    int port = pack.getPort();
                    
                    System.out.println("From "+port+" "+address2.getHostAddress());
                    
                    String msg = new String(receiveBuffer, 0, pack.getLength());
                    System.out.println("Message received: " + msg);
                }
            } catch (java.net.SocketTimeoutException e) {
                if (logger.isLoggable(Level.FINE)){
                    logger.log(Level.FINE, "timeout",e);
                }
            } catch (IOException e) {
                logger.log(Level.INFO, "error ?", e);
            }
        }
    }

}
