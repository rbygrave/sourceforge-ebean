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
package com.avaje.ebeaninternal.server.transaction;

import java.io.Serializable;
import java.util.ArrayList;

import com.avaje.ebean.event.BeanPersistListener;
import com.avaje.ebeaninternal.server.core.PersistRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

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

	private static final long serialVersionUID = 8389469180931531409L;

	/**
	 * The bean class name.
	 */
	private final String beanType;
	
	private ArrayList<Serializable> insertIds;
    private ArrayList<Serializable> updateIds;
    private ArrayList<Serializable> deleteIds;

	/**
	 * Create the payload.
	 */
	public RemoteBeanPersist(String beanType) {
		this.beanType = beanType;
	}

	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append(beanType);
	    if (insertIds != null){
	        sb.append(" insertIds:").append(insertIds);
	    }
	    if (updateIds != null){
            sb.append(" updateIds:").append(updateIds);
        }
	    if (deleteIds != null){
            sb.append(" deleteIds:").append(deleteIds);
        }
	    return sb.toString();
	}
	
	public void addId(PersistRequest.Type type, Serializable id){
	    switch (type) {
        case INSERT:
            addInsertId(id);
            break;
        case UPDATE:
            addUpdateId(id);
            break;
        case DELETE:
            addDeleteId(id);
            break;

        default:
            break;
        }
	}
	
	private void addInsertId(Serializable id) {
	    if (insertIds == null){
	        insertIds = new ArrayList<Serializable>();
	    }
	    insertIds.add(id);
	}
	
	private void addUpdateId(Serializable id) {
        if (updateIds == null){
            updateIds = new ArrayList<Serializable>();
        }
        updateIds.add(id);
    }
	
	private void addDeleteId(Serializable id) {
        if (deleteIds == null){
            deleteIds = new ArrayList<Serializable>();
        }
        deleteIds.add(id);
    }

	/**
	 * The bean class name.
	 */
	public String getBeanType() {
		return beanType;
	}
	
	/**
	 * Notify the cache and local BeanPersistListener of this event that came from another
	 * server in the cluster.
	 */
	public void notifyCacheAndListener(BeanDescriptor<?> desc) {
		
		BeanPersistListener<?> listener = desc.getPersistListener();
		
        if (insertIds != null) {
            // any insert invalidates the query cache
            desc.queryCacheClear();
            if (listener != null) {
                // notify listener
                for (int i = 0; i < insertIds.size(); i++) {
                    listener.remoteInsert(insertIds.get(i));
                }
            }
        }
        if (updateIds != null) {
            for (int i = 0; i < updateIds.size(); i++) {
                Serializable id = updateIds.get(i);

                // remove from cache
                desc.cacheRemove(id);
                if (listener != null) {
                    // notify listener
                    listener.remoteInsert(id);
                }
            }
        }
        if (deleteIds != null) {
            for (int i = 0; i < deleteIds.size(); i++) {
                Serializable id = deleteIds.get(i);

                // remove from cache
                desc.cacheRemove(id);
                if (listener != null) {
                    // notify listener
                    listener.remoteInsert(id);
                }
            }
        }

	}
}
