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

import com.avaje.ebean.event.BeanPersistListener;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Wraps the information representing a Inserted Updated or Deleted Bean.
 * <p>
 * This information is broadcast across the cluster so that remote BeanListeners
 * are notified of the inserts updates and deletes that occured.
 * </p>
 * <p>
 * You control it the data is broadcast and what data is broadcast by the
 * BeanListener.getClusterData() method. It is guessed that often just the Id
 * property or perhaps a few properties in a Map will be broadcast to reduce the
 * size of data sent around the network.
 * </p>
 */
public class RemoteBeanPersist implements Serializable {

	static final long serialVersionUID = 8389469180931531408L;

	/**
	 * PersistRequest.INSERT UDPATE or DELETE.
	 */
	private final PersistRequest.Type type;

	/**
	 * The bean class name.
	 */
	private final String beanType;

	/**
	 * The payload. Perhaps just the Id property.
	 */
	private final Serializable id;

	/**
	 * Create the payload.
	 */
	public RemoteBeanPersist(String beanType, PersistRequest.Type type, Serializable id) {
		this.beanType = beanType;
		this.type = type;
		this.id = id;
	}

	/**
	 * Return one of PersistRequest.INSERT UDPATE or DELETE.
	 */
	public PersistRequest.Type getType() {
		return type;
	}

	/**
	 * The bean class name.
	 */
	public String getBeanType() {
		return beanType;
	}

	/**
	 * The data which is typically just the Id property rather than the entire
	 * bean.
	 */
	public Serializable getId() {
		return id;
	}
	
	/**
	 * Notify the local BeanPersistListener of this event that came from another
	 * server in the cluster.
	 */
	public void notifyListener(BeanDescriptor<?> desc) {
		
		BeanPersistListener<?> listener = desc.getBeanPersistListener();
		if (listener != null){
			switch (type) {
			case INSERT:
				listener.remoteInsert(id);
				break;
	
			case UPDATE:
				listener.remoteUpdate(id);
				break;
	
			case DELETE:
				listener.remoteDelete(id);
				break;
	
			default:
				break;
			}
		}
	}
}
