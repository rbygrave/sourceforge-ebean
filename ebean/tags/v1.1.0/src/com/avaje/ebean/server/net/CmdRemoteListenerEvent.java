/**
 * Copyright (C) 2006  Robin Bygrave
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
package com.avaje.ebean.server.net;

import com.avaje.ebean.net.Command;
import com.avaje.ebean.net.CommandContext;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.transaction.RemoteListenerEvent;

/**
 * Cmd to send RemoteListenerEvent across the cluster.
 * <p>
 * Used to notify remote BeanListeners.
 * </p>
 */
public final class CmdRemoteListenerEvent extends Command {

	static final long serialVersionUID = -4576307772838335394L;
	
    RemoteListenerEvent event;
    
    /**
     * Create with a RemoteListenerEvent.
     */
    public CmdRemoteListenerEvent(RemoteListenerEvent event){
        super(null);
        this.event = event;
    }

    /**
     * Notify local BeanListeners of remote inserts updates and deletes.
     */
    public void execute(CommandContext context) {
        InternalEbeanServer server = context.getServer();
        server.remoteListenerEvent(event);
        
        event = null;
    }
    
    
}
