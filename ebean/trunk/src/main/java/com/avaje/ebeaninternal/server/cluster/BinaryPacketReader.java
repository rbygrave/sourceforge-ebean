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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.cluster.mcast.Packet;

public abstract class BinaryPacketReader {

    protected SpiEbeanServer server;
    
    public abstract SpiEbeanServer getEbeanServer(String serverName);
    
    public abstract Object readMessage(DataInput dataInput, int msgType) throws IOException;
    
    public List<Object> read(byte[] binaryData) throws IOException {
        
        ArrayList<Object> results = new ArrayList<Object>();
        
        ByteArrayInputStream bi = new ByteArrayInputStream(binaryData);
        DataInputStream dataInput = new DataInputStream(bi);
        
        Packet p = Packet.readHeader(dataInput);
        
        String serverName = p.getServerName();
        server = getEbeanServer(serverName);
        //p.setServer(server);
        
        boolean more = dataInput.readBoolean();
        while (more){
            int msgType = dataInput.readInt();
            Object o = readMessage(dataInput, msgType);
            results.add(o);
            // see if there is more information
            more = dataInput.readBoolean();
        }
        
        return results;
    }
    
}
