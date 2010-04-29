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
import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.server.cluster.BinaryMessage;

public class PacketMessages extends Packet {

    private final ArrayList<Message> messages;

    public static PacketMessages forWrite(long packetId, long timestamp, String serverName) throws IOException {
        return new PacketMessages(true, packetId, timestamp, serverName);
    }
    
    public static PacketMessages forRead(Packet header) throws IOException {
        return new PacketMessages(header);
    }
    
    private PacketMessages(boolean write, long packetId, long timestamp, String serverName) throws IOException {
        super(write, TYPE_MESSAGES, packetId, timestamp, serverName);
        this.messages = null;
    }

    private PacketMessages(Packet header) throws IOException {
        super(false, TYPE_MESSAGES, header.packetId, header.timestamp, header.serverName);
        this.messages = new ArrayList<Message>();
    }
    
    public List<Message> getMessages() {
        return messages;
    }

    protected void readMessage(DataInput dataInput, int msgType) throws IOException {
        
        switch (msgType) {
        case BinaryMessage.TYPE_MSGCONTROL:
            messages.add(MessageControl.readBinaryMessage(dataInput));
            break;

        case BinaryMessage.TYPE_MSGACK:
            messages.add(MessageAck.readBinaryMessage(dataInput));
            break;
            
        default:
            throw new RuntimeException("Invalid Transaction msgType "+msgType);
        }
    }
}
