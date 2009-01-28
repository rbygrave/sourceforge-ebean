/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package org.avaje.ebean.server.net;

import java.io.IOException;
import java.net.Socket;

/**
 * A TCP socket based client.
 */
public class SocketClient implements IoConnectionFactory {

    Endpoint endpoint;
    
    boolean online;
    
    /**
     * Create with host and port information.
     */
    public SocketClient(String host, int port) {
        endpoint = new Endpoint(host, port);
    }
    
    /**
     * Create without an Endpoint.
     */
    public SocketClient() {
        
    }
    
    /**
     * Return the endpoint.
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Set the endPoint.
     */
    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Return true if the client is thought to be online.
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Set whether the client is thought to be online.
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * Create a new connection.
     */
    public IoConnection createConnection(boolean secure) throws IOException {
        Socket socket = new Socket(endpoint.getHost(), endpoint.getPort());
        return new SocketConnection(socket);
    }
    
    
}
