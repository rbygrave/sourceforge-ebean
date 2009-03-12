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
package com.avaje.ebean.net;

import java.io.Serializable;

import com.avaje.ebean.Transaction;

/**
 * Base command object.
 */
public abstract class Command implements Serializable {
    
	private static final long serialVersionUID = 3061213493023459577L;

	String transactionId;
    
    Object responsePayload;
    
    /**
     * Create with an optional transactionId.
     */
    public Command(String transactionId) {
        this.transactionId = transactionId;
    }
    
    /**
     * Return the appropriate transaction from the context.
     */
    public Transaction getTransaction(CommandContext context) {
        return context.getTransMap().get(transactionId);
    }
    
    /**
     * Execute the command.
     */
    public abstract void execute(CommandContext context);
    
    /**
     * Get the response payload and place it back into the
     * original command.
     */
    public void mergeResponse(Command response){
        responsePayload = response.getResponsePayload();
    }

    /**
     * Return the response payload.
     */
    public Object getResponsePayload() {
        return responsePayload;
    }

    /**
     * Set the response payload.
     */
    public void setResponsePayload(Object responsePayload) {
        this.responsePayload = responsePayload;
    }
    
    
}
