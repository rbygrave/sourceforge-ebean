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

import com.avaje.ebean.config.GlobalProperties;

/**
 * Simple socket based server. 
 * <p>
 * Really just used for client server testing environment.
 * </p>
 */
public class ServerSocketMain {

	/**
	 * Start a Socket based server for some time.
	 */
	public static void main(String[] args) throws Exception {

		// default run for 60 seconds
		int sleep = GlobalProperties.getInt("ebean.serversocket.sleep",60000);
		
		// default listen on the port 10100
		int port = GlobalProperties.getInt("ebean.serversocket.port",10100);
		
        ServerSocketListener sl = new ServerSocketListener("ebsocket",port);
	    sl.startListening();
	      
	    Thread.sleep(sleep);
	}

}
