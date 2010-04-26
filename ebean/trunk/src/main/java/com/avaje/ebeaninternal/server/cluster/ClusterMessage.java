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
package com.avaje.ebeaninternal.server.cluster;

import java.io.Serializable;

import com.avaje.ebeaninternal.server.transaction.RemoteTransactionEvent;

/**
 * The messages broadcast around the cluster.
 */
public class ClusterMessage implements Serializable {

    private static final long serialVersionUID = 2993350408394934473L;
    
    private final String registerHost;

    private final String ebeanServer;

    private final boolean register;
    
    private final RemoteTransactionEvent transEvent;
    
    public static ClusterMessage register(String registerHost, boolean register){
        return new ClusterMessage(registerHost, register);
    }

    public static ClusterMessage transEvent(String ebeanServer, RemoteTransactionEvent transEvent){
        return new ClusterMessage(ebeanServer, transEvent);
    }
    
    /**
     * Used to construct a Child AttributeMap.
     */
    private ClusterMessage(String registerHost, boolean register) {
        this.registerHost = registerHost;
        this.register = register;
        this.ebeanServer = null;
        this.transEvent = null;
    }
    
    private ClusterMessage(String ebeanServer, RemoteTransactionEvent transEvent) {
        this.ebeanServer = ebeanServer;
        this.transEvent = transEvent;
        this.registerHost = null;
        this.register = false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (registerHost != null){
            sb.append("register ");
            sb.append(register);
            sb.append(" ");
            sb.append(registerHost);
        } else {
            sb.append("transEvent ");
            sb.append(ebeanServer).append(" ");
            sb.append(transEvent);
        }
        return sb.toString();
    }
    
    public boolean isRegisterEvent() {
        return registerHost != null;
    }

    public String getRegisterHost() {
        return registerHost;
    }

    public String getEbeanServer() {
        return ebeanServer;
    }

    public boolean isRegister() {
        return register;
    }

    public RemoteTransactionEvent getTransEvent() {
        return transEvent;
    }
   
}
