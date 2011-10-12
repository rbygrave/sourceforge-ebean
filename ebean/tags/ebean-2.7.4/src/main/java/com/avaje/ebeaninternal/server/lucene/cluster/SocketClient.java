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
package com.avaje.ebeaninternal.server.lucene.cluster;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The client side of the socket clustering.
 */
public class SocketClient {

    private static final Logger logger = Logger.getLogger(SocketClient.class.getName());
    
    private final InetSocketAddress address;
    
    private boolean keepAlive = false;
    
    private final String hostPort;
    
    private Socket socket;
    private OutputStream os;
    private InputStream is;

    private DataInput dataInput;
    private DataOutput dataOutput;
    
    /**
     * Construct with an IP address and port.
     */
    public SocketClient(InetSocketAddress address) {
        this.address = address;
        this.hostPort = address.getHostName()+":"+address.getPort();
    }

    public String getHostPort() {
        return hostPort;
    }
    
    public int getPort() {
        return address.getPort();
    }
    
    public OutputStream getOutputStream() {
        return os;
    }

    public InputStream getInputStream() {
        return is;
    }
    
    public DataInput getDataInput() {
        return dataInput;
    }

    public DataOutput getDataOutput() {
        return dataOutput;
    }

    public void reconnect() throws IOException {
        disconnect();
        connect();
    }
    
    public void connect() throws IOException {
        if (socket != null){
            throw new IllegalStateException("Already got a socket connection?");
        }
        Socket s = new Socket();
        s.setKeepAlive(keepAlive);
        s.connect(address);
        
        this.socket = s;
        this.os = socket.getOutputStream();
        this.is = socket.getInputStream();
    }
    
    public void initData() {
        dataOutput = new DataOutputStream(os);
        dataInput = new DataInputStream(is);
    }
    
    public void disconnect() {
        
        if (socket != null){
            
            try {
                socket.close();
            } catch (IOException e) {
                String msg = "Error disconnecting from Cluster member "+hostPort;
                logger.log(Level.INFO, msg, e);
            }
            
            os = null;
            socket = null;
        }
    }
    
    /**
     * Parse a host:port into a InetSocketAddress.
     */
    public static InetSocketAddress parseHostPort(String hostAndPort) {
        
        try {
            hostAndPort = hostAndPort.trim();
            int colonPos = hostAndPort.indexOf(":");
            if (colonPos == -1) {
                String msg = "No colon \":\" in "+hostAndPort;
                throw new IllegalArgumentException(msg);
            }
            String host = hostAndPort.substring(0, colonPos);
            String sPort = hostAndPort.substring(colonPos + 1, hostAndPort.length());
            int port = Integer.parseInt(sPort);
            
            return new InetSocketAddress(host, port);
            
        } catch (Exception ex){
            throw new RuntimeException("Error parsing ["+hostAndPort+"] for the form [host:port]", ex);
        }
    }
    
}
