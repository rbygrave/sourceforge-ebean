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
package com.avaje.ebeaninternal.server.net;

import java.net.Socket;


/**
 * Dispatcher that sets up ObjectInputStream for use with enhanced Beans.
 * <p>
 * Uses WrapIoConnection to setup BeanObjectInputStream.
 * </p>
 */
public class ServerDispatcher extends SocketDispatcher {

	/**
	 * Create for a socket.
	 */
	public ServerDispatcher(SocketListener listener, Socket clientSocket) {
		super(listener, clientSocket);
	}
	
	/**
	 * Create the headers setting up the ObjectInputStream.
	 */
	protected Headers readHeaders(SocketConnection request) throws Exception {
		
		return super.readHeaders(request);
	}

	
	
}
