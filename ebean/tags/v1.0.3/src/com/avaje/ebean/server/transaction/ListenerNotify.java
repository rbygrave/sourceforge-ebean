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

import com.avaje.ebean.bean.BeanListener;
import com.avaje.ebean.server.core.PersistRequest;
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

	//private final PluginCore pluginCore;
	
	private final DeploymentManager deploymentManager;
	
	/**
	 * Create with the TransactionManager.
	 */
	public ListenerNotify(TransactionManager manager, PluginCore pluginCore) {
		this.manager = manager;
		//this.pluginCore = pluginCore;
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

		// create a RemoteListenerEvent to send around the cluster
		RemoteListenerEvent remoteEvent = createRemoteListenerEvent(eventBeans);
		if (remoteEvent.size() > 0) {
			// make sure there is something to send
			manager.notifyCluster(remoteEvent);
		}

		// notify local BeanListeners
		ArrayList<PersistRequest> requests = eventBeans.getRequests();
		for (int i = 0; i < requests.size(); i++) {
			PersistRequest request = (PersistRequest) requests.get(i);
			localNotify(request);
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
		BeanManager mgr = deploymentManager.getBeanManager(typeDesc);
		
		BeanDescriptor desc = mgr.getBeanDescriptor();
		BeanListener beanListener = desc.getBeanListener();

		Serializable data = payload.getData();

		switch (type) {
		case INSERT:
			beanListener.inserted(false, data);
			break;

		case UPDATE:
			beanListener.inserted(false, data);
			break;

		case DELETE:
			beanListener.inserted(false, data);
			break;

		default:
			break;
		}
	}

	/**
	 * Create a RemoteListenerEvent that holds the payload for each inserted
	 * updated and deleted bean.
	 */
	private RemoteListenerEvent createRemoteListenerEvent(TransactionEventBeans eventBeans) {
		
		RemoteListenerEvent remoteEvent = new RemoteListenerEvent();
		
		ArrayList<PersistRequest> requests = eventBeans.getRequests();
		for (int i = 0; i < requests.size(); i++) {
			PersistRequest request = (PersistRequest) requests.get(i);
			createRemotePayload(remoteEvent, request);
		}

		return remoteEvent;
	}

	/**
	 * Create the payload for the PersistRequest.
	 */
	private void createRemotePayload(RemoteListenerEvent remoteEvent, PersistRequest request) {

		PersistRequest.Type type = request.getType();
		BeanDescriptor desc = request.getBeanDescriptor();
		BeanListener listener = desc.getBeanListener();

		String typeDesc = desc.getFullName();
		Serializable data = listener.getClusterData(request.getBean());
		if (data != null) {
			RemoteListenerPayload payload = new RemoteListenerPayload(typeDesc, type, data);
			remoteEvent.add(payload);
		}
	}

	private void localNotify(PersistRequest request) {

		BeanDescriptor desc = request.getBeanDescriptor();
		BeanListener listener = desc.getBeanListener();

		PersistRequest.Type type = request.getType();

		switch (type) {
		case INSERT:
			listener.inserted(true, request.getBean());
			break;

		case UPDATE:
			listener.updated(true, request.getBean());
			break;

		case DELETE:
			listener.deleted(true, request.getBean());
			break;

		default:
			break;
		}
	}
}
