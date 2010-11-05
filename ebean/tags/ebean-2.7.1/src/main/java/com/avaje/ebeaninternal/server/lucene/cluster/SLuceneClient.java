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
package com.avaje.ebeaninternal.server.lucene.cluster;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import com.avaje.ebeaninternal.server.lucene.LIndex;

public class SLuceneClient {

    private final String serverName;
    
    private final LIndex index;
    
    private final long localVersion;
    private long remoteVersion;
    
    private final SocketClient client;
    
    public SLuceneClient(String serverName, SocketClient client, long localVersion, LIndex index) {
        this.serverName = serverName;
        this.client = client;
        this.localVersion = localVersion;
        this.index = index;
    }
    
    public void setRemoteVersion(long remoteVersion) {
        this.remoteVersion = remoteVersion;
    }

    public LIndex getIndex() {
        return index;
    }

    public void disconnect() {
        client.disconnect();
    }
    
    public SocketClient getSocketClient() {
        return client;
    }
    
    private void sendMessageHeader(short msgType, long version) throws IOException {
        
        client.connect();
        client.initData();
        
        DataOutput dataOutput = client.getDataOutput();
        dataOutput.writeUTF(serverName);
        dataOutput.writeShort(msgType);
        dataOutput.writeUTF(index.getName());
        dataOutput.writeLong(version);
    }

    private boolean sendMessageHeader2(short msgType, long version) throws IOException {
        sendMessageHeader(msgType, version);
        client.getOutputStream().flush();
        return client.getDataInput().readBoolean();        
    }
    

    public boolean sendObtainCommit() throws IOException {
        return sendMessageHeader2(SLuceneSocketMessageTypes.OBTAIN_COMMIT, localVersion);
    }

    public void sendRelease() throws IOException {
        sendMessageHeader2(SLuceneSocketMessageTypes.RELEASE_COMMIT, remoteVersion);
    }
    
    public InputStream sendGetFile(String fileName) throws IOException {
        
        sendMessageHeader(SLuceneSocketMessageTypes.GET_FILE, remoteVersion);
        client.getDataOutput().writeUTF(fileName);
        client.getOutputStream().flush();
        boolean exists = client.getDataInput().readBoolean();
        if (!exists){
            return null;
        }
        return client.getInputStream();        
    }
}
