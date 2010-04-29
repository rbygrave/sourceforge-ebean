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

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OutgoingPacketsCache {

    private final Map<Long, Packet> packetMap = new TreeMap<Long, Packet>();

    public int size() {
        return packetMap.size();
    }
    
    public Packet getPacket(Long packetId){
        return packetMap.get(packetId);
    }
    
    public String toString() {
        return packetMap.keySet().toString();
    }
    
    public void registerPackets(List<Packet> packets) {
        for (int i = 0; i < packets.size(); i++) {
            Packet p = packets.get(i);
            packetMap.put(p.getPacketId(), p);
        }
    }

    public int trimAll() {
        int size = packetMap.size();
        packetMap.clear();
        return size;
    }
    
    public void trimAcknowledgedMessages(long minAcked) {
        //if (minAcked > 0){
            Iterator<Long> it = packetMap.keySet().iterator();
            while (it.hasNext()) {
                Long pktId = it.next();
                if (minAcked >= pktId.longValue()) {
                    System.out.println(" -- removing packet from cache,  newMin["+minAcked+"] >= " + pktId);
                    it.remove();
                } else {
                    System.out.println(" -- leaving packet in cache,  newMin["+minAcked+"] < " + pktId);
                }
            }
            System.out.println(" -- packetCacheSize: " + packetMap.size());
        //}
    }

    
    
}
