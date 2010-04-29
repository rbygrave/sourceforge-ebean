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

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import com.avaje.ebeaninternal.server.cluster.BinaryMessage;

public class Packet {
    
    public static final short TYPE_MESSAGES = 1;
    public static final short TYPE_TRANSEVENT = 2;

    protected short packetType;
    protected long packetId;
    protected long timestamp;
    protected String serverName;
    
    protected ByteArrayOutputStream buffer;
    protected DataOutputStream dataOut;
    protected byte[] bytes;
    
    private int resendCount;
    
    public static Packet forWrite(short packetType, long packetId, long timestamp, String serverName) throws IOException {
        return new Packet(true, packetType, packetId, timestamp, serverName);
    }
    
    public static Packet readHeader(DataInput dataInput) throws IOException {
        
        short packetType = dataInput.readShort();
        long packetId = dataInput.readLong();
        long timestamp = dataInput.readLong();
        String serverName = dataInput.readUTF();
        
        return new Packet(false, packetType, packetId, timestamp, serverName);
    }
    
    protected Packet(boolean write, short packetType, long packetId, long timestamp, String serverName) throws IOException{
        this.packetType = packetType;
        this.packetId = packetId;
        this.timestamp = timestamp;
        this.serverName = serverName;
        if (write){
            this.buffer = new ByteArrayOutputStream();
            this.dataOut = new DataOutputStream(buffer);
            writeHeader();
        } else {
            this.buffer = null;
            this.dataOut = null;
        }
    }

    private void writeHeader() throws IOException {
        dataOut.writeShort(packetType);
        dataOut.writeLong(packetId);
        dataOut.writeLong(timestamp);
        dataOut.writeUTF(serverName);
    }

    public int incrementResendCount() {
        return resendCount++;
    }
    
    public short getPacketType() {
        return packetType;
    }

    public long getPacketId() {
        return packetId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }

    public String getServerName() {
        return serverName;
    }
    
    public void writeEof() throws IOException {
        dataOut.writeBoolean(false);
    }
    
    public void read(DataInput dataInput) throws IOException {
        boolean more = dataInput.readBoolean();
        while (more){
            int msgType = dataInput.readInt();
            readMessage(dataInput, msgType);
            // see if there is more information
            more = dataInput.readBoolean();
        }
    }
    
    protected void readMessage(DataInput dataInput, int msgType) throws IOException {
        
    }
    
    public boolean writeBinaryMessage(BinaryMessage msg) throws IOException {
        
        byte[] bytes = msg.getByteArray();
        
        if (bytes.length + buffer.size() > 1000){
            // 0 = no more messages
            dataOut.writeBoolean(false);
            return false;
        }
        // 1 = another message
        dataOut.writeBoolean(true);
        dataOut.write(bytes);
        return true;
    }

    public byte[] getBytes() {
        if (bytes == null){
            bytes = buffer.toByteArray();
            buffer = null;
            dataOut = null;
        }
        return bytes;
    }

    
}
