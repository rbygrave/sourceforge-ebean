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

import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.avaje.ebeaninternal.api.TransactionEventTable;
import com.avaje.ebeaninternal.server.cluster.BinaryMessageList;
import com.avaje.ebeaninternal.server.core.PersistRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

/**
 * Holds information for a transaction that is sent around the cluster.
 * <p>
 * Holds a collection of RemoteBeanPersist objects used to notify
 * BeanPersistListeners and potentially a TransactionEventTable.
 * </p>
 */
public final class RemoteTransactionEvent implements Serializable {

	private static final long serialVersionUID = 5790053761599631177L;

	private final String serverName;
	
	private Map<String,RemoteBeanPersist> beanMap;

	private TransactionEventTable tableEvents;

	public RemoteTransactionEvent(String serverName) {
	    this.serverName = serverName;
	}

    public void writeBinaryMessage(BinaryMessageList msgList) throws IOException {
        if (tableEvents != null){
            tableEvents.writeBinaryMessage(msgList);
        }
        if (beanMap != null){
            for (RemoteBeanPersist beanPersist : beanMap.values()) {
                beanPersist.writeBinaryMessage(msgList);
            }
        }
    }
	
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    if (beanMap != null){
	        sb.append(beanMap.values());
	    }
	    if (tableEvents != null){
            sb.append(tableEvents);
        }
	    return sb.toString();
	}
	
	
	/**
	 * Return the EbeanServer name.
	 */
	public String getServerName() {
        return serverName;
    }

    /**
	 * Return true if this has some bean persist or table events.
	 */
	public boolean hasEvents() {
		return (beanMap != null && !beanMap.isEmpty()) || tableEvents != null;
	}

	/**
	 * Set the table events.
	 */
	public void setTableEvents(TransactionEventTable tableEvents) {
		this.tableEvents = tableEvents;
	}

	/**
	 * Return the table events if there where any.
	 */
	public TransactionEventTable getTableEvents() {
		return tableEvents;
	}
       
	/**
	 * Add a Insert Update or Delete payload.
	 */
	public void add(BeanDescriptor<?> desc, PersistRequest.Type type, Object id) {
	    if (beanMap == null){
	        beanMap = new LinkedHashMap<String, RemoteBeanPersist>();
	    }
	    String beanType = desc.getFullName();
	    RemoteBeanPersist r = beanMap.get(beanType);
		if (r == null){
			r = new RemoteBeanPersist(desc);
			beanMap.put(beanType, r);
		}
		r.addId(type, (Serializable)id);
	}

    public Map<String, RemoteBeanPersist> getBeanMap() {
        return beanMap;
    }
	
}
