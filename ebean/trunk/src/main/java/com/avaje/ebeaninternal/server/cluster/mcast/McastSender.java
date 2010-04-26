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
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class McastSender {

        private final int port;// = 7778;

        private final InetAddress inetAddress;

        private final DatagramSocket sock;
        
        private final InetSocketAddress sendAddr;
        
        public McastSender(int port, String address, int sendPort, String sendAddress){

            try {
                this.port = port;
                this.inetAddress = InetAddress.getByName( address );
                
                //InetAddress local = ;
                
                
                InetAddress sendInetAddress = null;
                if (sendAddress != null){
                    sendInetAddress = InetAddress.getByName(sendAddress);
                } else {
                    sendInetAddress = InetAddress.getLocalHost();
                }
                
                System.out.println("--------sendPort: "+sendPort+"  sendInetAddress: "+sendInetAddress);
                
                //this.sendAddr = new InetSocketAddress(local, sendPort);
                if (sendPort > 0){
                    this.sock = new DatagramSocket(sendPort,sendInetAddress);
                } else {
                    this.sock = new DatagramSocket(new InetSocketAddress(sendInetAddress, 0));
                }
                
                int localPort = sock.getLocalPort();
                System.out.println("Sender Bound to localPort:"+localPort+" "+sendInetAddress.getHostAddress());
                
                this.sendAddr = new InetSocketAddress(sendInetAddress, localPort);
                
            } catch( Exception e ) {
                String msg = "McastSender port:"+port+" sendPort:"+sendPort+" "+address;
                throw new RuntimeException(msg, e);
            }
        }

        public InetSocketAddress getAddress() {
            return sendAddr;
        }
        
        public void sendMessage(String msg) throws IOException {

            byte[] buf = msg.getBytes();
            
            DatagramPacket pack = new DatagramPacket( buf, buf.length, inetAddress, port );
            sock.send(pack);
        }

//        /**
//         * Repeatedly sends simple IP Multicast messages to the specified port and
//         * address.
//         * 
//         * @param args Arguments are ignored.
//         */
//        public static void main( String[] args ) {
//
//            IPMulticastSender sender = new IPMulticastSender();
//
//            while( true ) {
//                sender.sendMessage();
//                try {
//                    Thread.sleep( SLEEP_MILLISECS );
//                } catch( Exception e ) {
//                    // do nothing
//                }
//            }
//        }

    }
