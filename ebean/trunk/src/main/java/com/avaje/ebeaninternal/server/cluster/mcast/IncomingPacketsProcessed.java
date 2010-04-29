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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class IncomingPacketsProcessed {

    private final ConcurrentHashMap<String,GotAllPoint> mapByMember = new ConcurrentHashMap<String,GotAllPoint>();

    public void removeMember(String memberKey) {
        mapByMember.remove(memberKey);
    }
    
    /**
     * Return true if we should process this packet.
     * Return false if we have already processed the packet.
     */
    public boolean isProcessPacket(String memberKey, long packetId){
        
        GotAllPoint memberPackets = getMemberPackets(memberKey);
        return memberPackets.processPacket(packetId);
    }

    public AckResendMessages getAckResendMessages(IncomingPacketsLastAck lastAck) {
                    
        // Called by the McastClusterBroadcast manager thread
        
        AckResendMessages response = new AckResendMessages();
        
        for (GotAllPoint member : mapByMember.values()) {
            
            MessageAck lastAckMessage = lastAck.getLastAck(member.getMemberKey());
            
            member.addAckResendMessages(response, lastAckMessage);
        }
        
        return response;
    }

    private GotAllPoint getMemberPackets(String memberKey) {
        // This method is only called single threaded 
        // by the listener thread so I'm happy that this
        // put into mapByMember is ok.
        GotAllPoint memberGotAllPoint = mapByMember.get(memberKey);
        if (memberGotAllPoint == null){
            memberGotAllPoint = new GotAllPoint(memberKey);
            mapByMember.put(memberKey, memberGotAllPoint);
        }
        return memberGotAllPoint;
    }

    
    /**
     * Keeps track of packets received from a particular member of 
     * the cluster.
     * <p>
     * It notes the packetIds of the packets received and uses those
     * to maintain the 'gotAllPoint'. The 'gotAllPoint' is the packetId
     * which we know we received all the previous packets. 
     * </p>
     */
    public static class GotAllPoint {

        private final String memberKey;

        private long gotAllPoint;
        
        private long gotMaxPoint;

        /**
         * Packets received out of order.
         */
        private ArrayList<Long> outOfOrderList = new ArrayList<Long>();

        public GotAllPoint(String memberKey) {
            this.memberKey = memberKey;
        }
        
        public void addAckResendMessages(AckResendMessages response, MessageAck lastAckMessage) {

            synchronized (this) {
                if (lastAckMessage != null && lastAckMessage.getGotAllPacketId() >= gotAllPoint){
                    // nothing has changed 
                } else {
                    response.add(new MessageAck(memberKey, gotAllPoint));
                }

                if (getMissingPacketCount() > 0) {
                    List<Long> missingPackets = getMissingPackets();
                    response.add(new MessageResend(memberKey, missingPackets));
                }
            }
        }
        
        public String getMemberKey() {
            return memberKey;
        }

        public long getGotAllPoint() {
            synchronized (this) {
                return gotAllPoint;
            }
        }

        public long getGotMaxPoint() {
            synchronized (this) {
                return gotMaxPoint;
            }
        }

        public int getMissingPacketCount() {
            synchronized (this) {
                if (gotMaxPoint <= gotAllPoint) {
                    return 0;
                }
                return (int) (gotMaxPoint - gotAllPoint) - outOfOrderList.size();
            }
        }

        public List<Long> getMissingPackets() {

            synchronized (this) {
                ArrayList<Long> missingList = new ArrayList<Long>();
    
                // this is not particularly efficient but expecting
                // the outOfOrderList to be relatively small
    
                for (long i = gotAllPoint + 1; i < gotMaxPoint; i++) {
                    Long packetId = Long.valueOf(i);
                    if (!outOfOrderList.contains(packetId)) {
                        missingList.add(packetId);
                    }
                }
    
                return missingList;
            }
        }
        
        public boolean processPacket(long packetId) {
            synchronized (this) {
                if (gotAllPoint == 0) {
                    gotAllPoint = packetId;
                    return true;
                }
                if (packetId <= gotAllPoint) {
                    // already processed this packet
                    return false;
                }
                if (packetId == gotAllPoint + 1) {
                    gotAllPoint = packetId;
                } else {
                    if (packetId > gotMaxPoint) {
                        gotMaxPoint = packetId;
                    }
                    outOfOrderList.add(Long.valueOf(packetId));
                }
                checkOutOfOrderList();
                return true;
            }
        }

        private void checkOutOfOrderList() {

            if (outOfOrderList.size() == 0){
                return;
            }
            
            boolean continueCheck = false;
            do {
                long nextPoint = gotAllPoint + 1;

                Iterator<Long> it = outOfOrderList.iterator();
                while (it.hasNext()) {
                    Long id = it.next();
                    if (id.longValue() == nextPoint) {
                        // we found the next one in the outOfOrderList
                        it.remove();
                        gotAllPoint = nextPoint;
                        continueCheck = true;
                        break;
                    }
                }
            } while (continueCheck);

        }

    }

}
