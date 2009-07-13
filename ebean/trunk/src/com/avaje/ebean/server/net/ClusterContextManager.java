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

import com.avaje.ebean.Ebean;
import com.avaje.ebean.internal.InternalEbeanServer;
import com.avaje.ebean.net.CommandContext;
import com.avaje.ebean.net.Constants;

/**
 * No session support for Cluster event processing.
 * <p>
 * Just server to server sending of CmdTransactionEvent.
 * </p>
 */
public class ClusterContextManager implements CommandContextManager, Constants {

	/**
	 * Create a Context for the appropriate EbeanServer.
	 */
    public CommandContext getContext(Headers headers) {
        
    	// the header has the name of the EbeanServer
        String name = headers.get(SERVER_NAME_KEY);
        
        InternalEbeanServer server = (InternalEbeanServer)Ebean.getServer(name);
        
        CommandContext context = new CommandContext();
        context.setServer(server);
        
        return context;
    }

    
    
}
