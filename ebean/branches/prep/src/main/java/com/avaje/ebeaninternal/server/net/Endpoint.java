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
package com.avaje.ebeaninternal.server.net;

/**
 * Represents an endpoint of a client server connection.
 */
public class Endpoint {

    /**
     * the hostname of the endpoint.
     */
    String host;
    /**
     * The socket port of the endpoint.
     */
    int port;
    /**
     * The hostname:port string.
     */
    String fullName;
    
    /**
     * Set to true when this is online.
     */
    boolean isOnline = false;
    
    /**
     * Create the end point.
     */
    public Endpoint(String host, int port){
        this.host = host;
        this.port = port;
        this.fullName = host+":"+port;
    }
    /**
     * Create parsing fullname in format hostname:port.
     */
    public Endpoint(String fullName) {
        this.fullName = fullName;
        parseFullName(fullName);
    }

    private void parseFullName(String fullName) {
        try {
        int colonPos = fullName.indexOf(":");
        if (colonPos > 0){
            host = fullName.substring(0,colonPos);
            String sPort = fullName.substring(colonPos+1,fullName.length());
            port = Integer.parseInt(sPort);
        }
        } catch (Exception ex){
            throw new RuntimeException("Error parsing ["+fullName+"] for the form [host:port]", ex);
        }
    }
    
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if (obj instanceof Endpoint){
            Endpoint nd = (Endpoint)obj;
            return nd.hashCode() == hashCode();
        }
        return false;
    }
    
    public int hashCode() {
        int hc = Endpoint.class.getName().hashCode();
        hc = 31*hc + fullName.hashCode();
        return hc;
    }
    
    public String toString() {
        return fullName;
    }
    
    /**
     * Returns the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port.
     */
    public int getPort() {
        return port;
    }
    /**
     * Returns host:port
     */
    public String getFullName() {
        return fullName;
    }
    /**
     * Return true if this endpoint is considered online.
     */
    public boolean isOnline() {
        return isOnline;
    }
    /**
     * Set to true when the endpoint is considered online.
     */
    public void setOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }
    
    
}
