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
package com.avaje.ebean.server.transaction;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * A collection of RemoteListenerPayload objects used to notify BeanListeners
 * across a cluster of Bean insert update and delete events.
 * <p>
 * It is probably not wise to send the entire bean around so instead the
 * RemoteListenerPayload has a Serializable payload which is perhaps the bean Id
 * property or a Map of properties that you wish to send across the cluster.
 * </p>
 * <p>
 * You control the payload data in the BeanListener.getClusterData() method.
 * </p>
 */
public class RemoteListenerEvent implements Serializable {

	static final long serialVersionUID = 5790053761599631176L;
	
	ArrayList<RemoteListenerPayload> list = new ArrayList<RemoteListenerPayload>();

	public RemoteListenerEvent() {

	}

	/**
	 * Add a Insert Update or Delete payload.
	 */
	public void add(RemoteListenerPayload payload) {
		list.add(payload);
	}

	/**
	 * Return the number of payload objects.
	 */
	public int size() {
		return list.size();
	}
	
	/**
	 * Return the list of RemoteListenerPayload.
	 */
	public ArrayList<RemoteListenerPayload> getPayloads() {
		return list;
	}
}
