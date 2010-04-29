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
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.api.SpiEbeanServer;

public class McastListener implements Runnable {

    private static final Logger logger = Logger.getLogger(McastListener.class.getName());

    private final McastClusterBroadcast owner;
    
    private final McastPacketControl packetControl;
    
    private final MulticastSocket sock;

    private final Thread listenerThread;

    private final String localSenderHostPort;

    private final InetAddress group;

    private final boolean debugIgnore;

    private DatagramPacket pack;

    private byte[] receiveBuffer;

    private volatile boolean doingShutdown;
    
    public McastListener(McastClusterBroadcast owner, McastPacketControl packetControl, int port, String address, 
            int bufferSize, int timeout, String localSenderHostPort, 
            boolean disableLoopback, int ttl, InetAddress mcastBindAddress) {

        this.debugIgnore = GlobalProperties.getBoolean("ebean.debug.mcast.ignore", false);

        this.owner = owner;
        this.packetControl = packetControl;
        this.localSenderHostPort = localSenderHostPort;
        this.receiveBuffer = new byte[bufferSize];
        this.listenerThread = new Thread(this, "EbeanClusterMcastListener");

        String msg = "Cluster Multicast Listening address["+address+"] port["+port+"] disableLoopback["+disableLoopback+"]";
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
            sock.setReuseAddress(true);
            pack = new DatagramPacket(receiveBuffer, receiveBuffer.length);
            sock.joinGroup(group);
            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startListening() {
        this.listenerThread.setDaemon(true);
        this.listenerThread.start();
        
        logger.info("Cluster Multicast Listener up and joined Group");
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

                    String senderHostPort = senderAddr.getAddress().getHostAddress()+":"+senderAddr.getPort();                    
                    
                    if (senderHostPort.equals(localSenderHostPort)){
                        if (debugIgnore || logger.isLoggable(Level.FINE)){
                            logger.info("Ignoring message as sent by localSender: "+localSenderHostPort);
                        }
                    } else {
                                                   
                        byte[] data = pack.getData();
                        ByteArrayInputStream bi = new ByteArrayInputStream(data);
                        DataInputStream dataInput = new DataInputStream(bi);
                        
                        Packet header = Packet.readHeader(dataInput);
                        
                        long packetId = header.getPacketId();
                        boolean ackMsg = packetId == 0;
                        
                        boolean processThisPacket = ackMsg || packetControl.isProcessPacket(senderHostPort, header.getPacketId());
                        
                        if (!processThisPacket){
                            if (debugIgnore || logger.isLoggable(Level.FINE)){
                                logger.info("Already processed packet: "+header.getPacketId());
                            }
                        } else {
                            if (logger.isLoggable(Level.FINER)){
                                logger.info("Incoming packet:"+header.getPacketId()+" type:"+header.getPacketType());
                            }
                                
                            processPacket(senderHostPort, header, dataInput);                            
                        }
                    }
                }
                
            } catch (java.net.SocketTimeoutException e) {
                
                packetControl.onListenerTimeout();
                
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "timeout", e);
                }
            } catch (IOException e) {
                logger.log(Level.INFO, "error ?", e);  
            } 
        }
    }

    protected void processPacket(String senderHostPort, Packet header, DataInput dataInput) {
        try {
            switch (header.getPacketType()) {
            case Packet.TYPE_MESSAGES:
                packetControl.processMessagesPacket(senderHostPort, header, dataInput);
                break;
                
            case Packet.TYPE_TRANSEVENT:
                processTransEventPacket(header, dataInput);
                break;
                
            default:
                String msg = "Unknown Packet type:" + header.getPacketType();
                logger.log(Level.SEVERE, msg);
                break;
            }
        } catch (IOException e) {
            // need to ask to get this packet resent...
            String msg = "Error reading Packet " + header.getPacketId() + " type:" + header.getPacketType();
            logger.log(Level.SEVERE, msg, e);
        }
    }
    
    private void processTransEventPacket(Packet header, DataInput dataInput) throws IOException {

        SpiEbeanServer server = owner.getEbeanServer(header.getServerName());

        PacketTransactionEvent tranEventPacket = PacketTransactionEvent.forRead(header, server);
        tranEventPacket.read(dataInput);

        server.remoteTransactionEvent(tranEventPacket.getEvent());
    }


}
