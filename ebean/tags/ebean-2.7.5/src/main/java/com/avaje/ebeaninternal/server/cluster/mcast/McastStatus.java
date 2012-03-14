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

/**
 * Gives an overall status of this Cluster instance.
 * <p>
 * Ideally you want to see relatively low Re-send statistics.
 * </p>
 * 
 * @author rbygrave
 *
 */
public class McastStatus {

    private final long totalTxnEventsSent;
    private final long totalTxnEventsReceived;
    
    private final long totalPacketsSent;
    private final long totalPacketsResent;
    private final long totalPacketsReceived;
    
    private final long totalBytesSent;
    private final long totalBytesResent;
    private final long totalBytesReceived;

    private final int currentGroupSize;
    private final int outgoingPacketsCacheSize;
    
    private final long currentPacketId;
    private final long minAckedPacketId;
    private final String lastOutgoingAcks;
    
    public String getSummary() {
        
        StringBuilder sb = new StringBuilder(80);
        sb.append("txnOut:").append(totalTxnEventsSent).append("; ");
        sb.append("txnIn:").append(totalTxnEventsReceived).append("; ");
        sb.append("outPackets:").append(totalPacketsSent).append("; ");
        sb.append("outBytes:").append(totalBytesSent).append("; ");
        sb.append("inPackets:").append(totalPacketsReceived).append("; ");
        sb.append("inBytes:").append(totalBytesReceived).append("; ");
        sb.append("resentPackets:").append(totalPacketsResent).append("; ");
        sb.append("resentBytes:").append(totalBytesResent).append("; ");
        sb.append("groupSize:").append(currentGroupSize).append("; ");
        sb.append("cache:").append(outgoingPacketsCacheSize).append("; ");
        sb.append("currentPacket:").append(currentPacketId).append("; ");
        sb.append("minAckedPacket:").append(minAckedPacketId).append("; ");
        sb.append("lastAck:").append(lastOutgoingAcks).append("; ");

        return sb.toString();
    }
    
    public McastStatus(int currentGroupSize,
            int outgoingPacketsCacheSize,
            long currentPacketId,
            long minAckedPacketId,
            String lastOutgoingAcks,
            long totalTransEventsSent,
            long totalTransEventsReceived,
            long totalPacketsSent,
            long totalPacketsResent,
            long totalPacketsReceived,
            long totalBytesSent,
            long totalBytesResent,
            long totalBytesReceived) {
        
        this.currentGroupSize = currentGroupSize;
        this.outgoingPacketsCacheSize = outgoingPacketsCacheSize;
        this.currentPacketId = currentPacketId;
        this.minAckedPacketId = minAckedPacketId;
        this.lastOutgoingAcks = lastOutgoingAcks;
        this.totalTxnEventsSent = totalTransEventsSent;
        this.totalTxnEventsReceived = totalTransEventsReceived;
        this.totalPacketsSent = totalPacketsSent;
        this.totalPacketsResent = totalPacketsResent;
        this.totalPacketsReceived = totalPacketsReceived;

        this.totalBytesSent = totalBytesSent;
        this.totalBytesResent = totalBytesResent;
        this.totalBytesReceived = totalBytesReceived;

    }

    
    public long getTotalTxnEventsReceived() {
        return totalTxnEventsReceived;
    }

    public long getTotalPacketsReceived() {
        return totalPacketsReceived;
    }

    public long getTotalBytesSent() {
        return totalBytesSent;
    }

    public long getTotalBytesResent() {
        return totalBytesResent;
    }

    public long getTotalBytesReceived() {
        return totalBytesReceived;
    }

    public String getLastOutgoingAcks() {
        return lastOutgoingAcks;
    }

    public int getOutgoingPacketsCacheSize() {
        return outgoingPacketsCacheSize;
    }

    public long getCurrentPacketId() {
        return currentPacketId;
    }

    public long getMinAckedPacketId() {
        return minAckedPacketId;
    }

    public long getTotalTxnEventsSent() {
        return totalTxnEventsSent;
    }

    public long getTotalPacketsSent() {
        return totalPacketsSent;
    }

    public long getTotalPacketsResent() {
        return totalPacketsResent;
    }

    public long getCurrentGroupSize() {
        return currentGroupSize;
    }
    
}
