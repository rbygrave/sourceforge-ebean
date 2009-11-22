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
package com.avaje.ebean.server.net;

import java.io.IOException;
import java.net.Socket;

/**
 * The client side of a TCP Sockect connection.
 */
public class SocketConnection extends IoConnection {

    /**
     * The underlying socket.
     */
    Socket socket;
   
    /**
     * Create for a given Socket.
     */
    public SocketConnection(Socket socket) throws IOException {
        super(socket.getInputStream(), socket.getOutputStream());
        this.socket = socket;
    }
    
    /**
     * Disconnect from the server. 
     */
    public void disconnect() throws IOException {
        os.flush();
        socket.close();
    }



}
