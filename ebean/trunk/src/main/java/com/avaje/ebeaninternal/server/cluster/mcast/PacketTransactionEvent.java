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

import java.io.DataInput;
import java.io.IOException;

import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;
import com.avaje.ebeaninternal.server.cluster.BinaryMessage;
import com.avaje.ebeaninternal.server.transaction.RemoteBeanPersist;
import com.avaje.ebeaninternal.server.transaction.RemoteTransactionEventReceived;

public class PacketTransactionEvent extends Packet {

    private final SpiEbeanServer server;
    
    private final RemoteTransactionEventReceived event;

    public static PacketTransactionEvent forWrite(long packetId, long timestamp, String serverName) throws IOException {
        return new PacketTransactionEvent(true, packetId, timestamp, serverName);
    }
    
    private PacketTransactionEvent(boolean write, long packetId, long timestamp, String serverName) throws IOException {
        super(write, TYPE_TRANSEVENT, packetId, timestamp, serverName);
        this.server = null;
        this.event = null;
    }

    private PacketTransactionEvent(Packet header, SpiEbeanServer server) throws IOException {
        super(false, TYPE_TRANSEVENT, header.packetId, header.timestamp, header.serverName);
        this.server = server;
        this.event = new RemoteTransactionEventReceived(server);
    }

    public static PacketTransactionEvent forRead(Packet header, SpiEbeanServer server) throws IOException {
        return new PacketTransactionEvent(header, server);
    }
 
    public RemoteTransactionEventReceived getEvent() {
        return event;
    }

    protected void readMessage(DataInput dataInput, int msgType) throws IOException {
        
        switch (msgType) {
        case BinaryMessage.TYPE_BEANIUD:
            event.add(RemoteBeanPersist.readBinaryMessage(server, dataInput));
            break;
            
        case BinaryMessage.TYPE_TABLEIUD:
            event.add(TableIUD.readBinaryMessage(dataInput));
            break;
            
        default:
            throw new RuntimeException("Invalid Transaction msgType "+msgType);
        }
    }
    
}
