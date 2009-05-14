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

import java.util.ArrayList;

import com.avaje.ebean.bean.BeanPersistListener;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.deploy.DeploymentManager;
import com.avaje.ebean.server.plugin.PluginCore;

/**
 * Helper for TransactionManager to notify BeanListeners.
 * <p>
 * It is worth noting that BeanListeners could take some time to process
 * depending on what they do. As such this processing occurs in background
 * threads.
 * </p>
 */
public class ListenerNotify {

	private final TransactionManager manager;
	
	private final DeploymentManager deploymentManager;
	
	/**
	 * Create with the TransactionManager.
	 */
	public ListenerNotify(TransactionManager manager, PluginCore pluginCore) {
		this.manager = manager;
		this.deploymentManager = pluginCore.getDeploymentManager();
	}

	/**
	 * For local transaction events notify BeanListeners.
	 * <p>
	 * This also needs to create a RemoteListenerEvent and broadcast that across
	 * the cluster.
	 * </p>
	 */
	public void localNotify(TransactionEventBeans eventBeans) {

		RemoteListenerEvent remoteEvent = new RemoteListenerEvent();

		// notify BeanPersistListener
		ArrayList<PersistRequestBean<?>> requests = eventBeans.getRequests();
		for (int i = 0; i < requests.size(); i++) {
			PersistRequestBean<?> request = requests.get(i);
			if (localNotify(request)) {
				// notify the cluster of this event
				remoteEvent.add(request.createRemoteListenerPayload());
			}
		}

		if (remoteEvent.size() > 0) {
			// send the interesting events to the cluster
			manager.notifyCluster(remoteEvent);
		}
	}

	/**
	 * Notify local BeanListeners of events from another server in the cluster.
	 */
	public void remoteNotify(RemoteListenerEvent remoteEvent) {

		ArrayList<RemoteListenerPayload> list = remoteEvent.getPayloads();
		for (int i = 0; i < list.size(); i++) {
			remoteNotify((RemoteListenerPayload) list.get(i));
		}
	}

	private void remoteNotify(RemoteListenerPayload payload) {

		PersistRequest.Type type = payload.getType();
		String typeDesc = payload.getTypeDescription();
		BeanManager<?> mgr = deploymentManager.getBeanManager(typeDesc);
		BeanDescriptor<?> desc = mgr.getBeanDescriptor();
		BeanPersistListener<?> beanListener = desc.getBeanListener();

		Object id = payload.getId();

		switch (type) {
		case INSERT:
			beanListener.remoteInsert(id);
			break;

		case UPDATE:
			beanListener.remoteUpdate(id);
			break;

		case DELETE:
			beanListener.remoteDelete(id);
			break;

		default:
			break;
		}
	}

	private <T> boolean localNotify(PersistRequestBean<T> request) {

		BeanDescriptor<T> desc = request.getBeanDescriptor();
		BeanPersistListener<T> listener = desc.getBeanListener();

		PersistRequest.Type type = request.getType();

		switch (type) {
		case INSERT:
			return listener.inserted(request.getBean());

		case UPDATE:
			return listener.updated(request.getBean(), request.getUpdatedProperties());

		case DELETE:
			return listener.deleted(request.getBean());

		default:
			return false;
		}
	}
}
