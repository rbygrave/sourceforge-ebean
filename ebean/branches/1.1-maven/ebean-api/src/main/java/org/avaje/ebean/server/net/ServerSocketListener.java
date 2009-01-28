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
package org.avaje.ebean.server.net;

import java.io.IOException;
import java.net.Socket;


/**
 * SocketListener setup for use with CommandProcessor and enhanced ObjectInputStreams.
 */
public class ServerSocketListener extends SocketListener {

	CommandProcessor processor;
	
	/**
	 * Create listening on a given port and name for the thread pool.
	 */
    public ServerSocketListener(String threadPoolName, int port) {
		super(threadPoolName, port);
		
		processor = new CommandProcessor();
		processor.setUseSessionId(true);
		processor.setContextManager(new ClusterContextManager());
	}
        
    /**
     * Return the processor to process the command.
     */
	public ConnectionProcessor getRequestProcessor(String serviceKey) throws IOException {
		return processor;
	}

	/**
	 * Create a Runnable to process the command.
	 */
	protected Runnable createRunnable(SocketListener listener, Socket clientSocket) {
		return new ServerDispatcher(listener, clientSocket);
	}
	
}
