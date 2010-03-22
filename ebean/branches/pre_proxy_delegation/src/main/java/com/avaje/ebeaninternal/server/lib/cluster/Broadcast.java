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
package com.avaje.ebeaninternal.server.lib.cluster;

import java.io.Serializable;

import com.avaje.ebeaninternal.server.net.ConnectionProcessor;
import com.avaje.ebeaninternal.server.net.Endpoint;
import com.avaje.ebeaninternal.server.net.Headers;
import com.avaje.ebeaninternal.server.net.IoConnection;

/**
 * Sends messages to the cluster members.
 */
public abstract class Broadcast implements ConnectionProcessor {

	/**
	 * Register this member with the other cluster members.
	 * 
	 * @param local
	 *            the local member endpoint
	 * @param others
	 *            the cluster members
	 */
	public abstract void register(Endpoint local, Endpoint[] others);

	/**
	 * Send a message to all the members of the cluster.
	 */
	public abstract boolean broadcast(Headers headers, Serializable payload);

	/**
	 * Deregister from the cluster. Tell all the cluster members that you are
	 * shutting down.
	 */
	public abstract void deregister();

	/**
	 * Process a cluster register or deregister message.
	 */
	public abstract void process(IoConnection request);

}
