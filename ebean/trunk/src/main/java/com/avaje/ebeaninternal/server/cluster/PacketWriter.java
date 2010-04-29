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
package com.avaje.ebeaninternal.server.cluster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.server.cluster.mcast.Packet;

public abstract class PacketWriter {

    
    protected PacketWriter() {
    }
    
    protected abstract long nextPacketId();

    protected abstract Packet createPacket(long packetId, long timestamp, String serverName) throws IOException;
    
    public List<Packet> write(boolean requiresAck, BinaryMessageList messageList, String serverName) throws IOException {

        List<BinaryMessage> list = messageList.getList();

        ArrayList<Packet> packets = new ArrayList<Packet>(1);

        long timestamp = System.currentTimeMillis();

        long packetId = requiresAck ? nextPacketId() : 0; 
        Packet p = createPacket(packetId, timestamp, serverName);
        packets.add(p);

        for (int i = 0; i < list.size(); i++) {
            BinaryMessage binMsg = list.get(i);
            if (!p.writeBinaryMessage(binMsg)) {
                // didn't fit into the package so put into another packet
                packetId = requiresAck ? nextPacketId() : 0;
                p = createPacket(packetId, timestamp, serverName);
                packets.add(p);
                p.writeBinaryMessage(binMsg);
            }
        }
        p.writeEof();
        
        return packets;

    }

}
