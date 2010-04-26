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
import java.util.logging.Logger;

public class McastSender {

    private static final Logger logger = Logger.getLogger(McastSender.class.getName());

    private final int port;

    private final InetAddress inetAddress;

    private final DatagramSocket sock;

    private final InetSocketAddress sendAddr;

    public McastSender(int port, String address, int sendPort, String sendAddress) {

        try {
            this.port = port;
            this.inetAddress = InetAddress.getByName(address);

            InetAddress sendInetAddress = null;
            if (sendAddress != null) {
                sendInetAddress = InetAddress.getByName(sendAddress);
            } else {
                sendInetAddress = InetAddress.getLocalHost();
            }

            if (sendPort > 0) {
                this.sock = new DatagramSocket(sendPort, sendInetAddress);
            } else {
                this.sock = new DatagramSocket(new InetSocketAddress(sendInetAddress, 0));
            }

            String msg = "Cluster Multicast Sender on["+sendInetAddress.getHostAddress()+":"+sock.getLocalPort()+"]";
            logger.info(msg);

            this.sendAddr = new InetSocketAddress(sendInetAddress, sock.getLocalPort());

        } catch (Exception e) {
            String msg = "McastSender port:" + port + " sendPort:" + sendPort + " " + address;
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Return the send Address so that if we have loopback messages we can
     * detect if they where sent by this local sender and hence should be
     * ignored.
     */
    public InetSocketAddress getAddress() {
        return sendAddr;
    }

    public void sendMessage(byte[] byteMsg) throws IOException {

        DatagramPacket pack = new DatagramPacket(byteMsg, byteMsg.length, inetAddress, port);
        sock.send(pack);
    }
}
