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
package com.avaje.ebean.bean;

import java.io.Serializable;

/**
 * Listens for committed bean events.
 * <p>
 * These listen events occur after a successful commit. They also occur in a
 * background thread rather than the thread used to perform the actual insert
 * update or delete. In this way there is a delay between the commit and when
 * the listener is notified of the event.
 * </p>
 * <p>
 * For a cluster these events may need to be broadcast. The getClusterData()
 * defines what information is sent across the cluster as generally you don't
 * want to send the entire bean but perhaps just the Id.
 * </p>
 * <p>
 * If getClusterData() returns null then the event is NOT broadcast across the
 * cluster.
 * </p>
 * <p>
 * It is worth noting that BeanListener is different in three main ways from
 * BeanController postXXX methods.
 * <ul>
 * <li>BeanListener only sees successfully committed events. BeanController pre
 * and post methods occur before the commit or a rollback and will see events
 * that are later rolled back</li>
 * <li>BeanListener runs in a background thread and will not effect the
 * response time of the actual persist where as BeanController code will</li>
 * <li>BeanListener can be notified of events from other servers in a cluster.</li>
 * </ul>
 * </p>
 */
public interface BeanListener {

	/**
	 * The types of entity bean this is the listener for.
	 */
	public Class<?>[] registerFor();
	
	/**
	 * For the inserted updated or deleted bean return the data which you want
	 * to send across the cluster.
	 * <p>
	 * This cluster data is the object that gets passed to the inserted updated
	 * deleted methods on the BeanListeners on the other servers in the cluster.
	 * </p>
	 * <p>
	 * If getClusterData() returns null then this event is not broadcast across
	 * the cluster. You will not get notified of matching events on other
	 * servers in the cluster.
	 * </p>
	 * <p>
	 * You will typically want to just send the Id property or perhaps several
	 * properties in a Map rather than the whole bean.
	 * </p>
	 * 
	 * @param bean
	 *            the bean that was inserted updated or deleted
	 * @return the data that gets sent across the cluster
	 */
	public Serializable getClusterData(Object bean);

	/**
	 * Notified that a bean has been inserted successfully.
	 * <p>
	 * The data object is the actual bean for local events. If the event has
	 * come from the cluster then the data object is a getClusterData() object.
	 * This could be just the Id property for example or a Map of the properties
	 * you wish to broadcast.
	 * </p>
	 * 
	 * @param local
	 *            flag to indicate if this is a local or cluster event
	 * @param data
	 *            if local this is the bean, otherwise the getClusterData()
	 *            object.
	 */
	public void inserted(boolean local, Object data);

	/**
	 * Notified that a bean has been updated successfully.
	 * 
	 * @param local
	 *            flag to indicate if this is a local or cluster event
	 * @param data
	 *            if local this is the bean, otherwise the getClusterData()
	 *            object.
	 */
	public void updated(boolean local, Object data);

	/**
	 * Notified that a bean has been deleted successfully.
	 * 
	 * @param local
	 *            flag to indicate if this is a local or cluster event
	 * @param data
	 *            if local this is the bean, otherwise the getClusterData()
	 *            object.
	 */
	public void deleted(boolean local, Object data);

}
