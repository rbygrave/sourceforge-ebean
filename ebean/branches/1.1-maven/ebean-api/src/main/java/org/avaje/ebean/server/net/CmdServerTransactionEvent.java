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
import com.avaje.ebean.server.transaction.TransactionEvent;

/**
 * A TransactionEvent that is broadcast across the cluster.
 */
public final class CmdServerTransactionEvent extends Command {

    static final long serialVersionUID = 5913101901827738603L;
    
    TransactionEvent event;
    
    /**
     * Create with a TransactionEvent holding the external modifications.
     */
    public CmdServerTransactionEvent(TransactionEvent event){
        super(null);
        this.event = event;
    }

    public void execute(CommandContext context) {
        InternalEbeanServer server = context.getServer();
        server.externalModification(event);
        event = null;
    }

}
