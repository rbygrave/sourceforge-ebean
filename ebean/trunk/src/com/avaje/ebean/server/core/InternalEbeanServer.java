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
package com.avaje.ebean.server.core;

import java.util.Iterator;

import javax.management.MBeanServer;

import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.TxScope;
import com.avaje.ebean.bean.ScopeTrans;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.plugin.Plugin;
import com.avaje.ebean.server.query.CQuery;
import com.avaje.ebean.server.query.CQueryEngine;
import com.avaje.ebean.server.transaction.RemoteListenerEvent;
import com.avaje.ebean.server.transaction.TransactionEvent;
import com.avaje.ebean.util.InternalEbean;

/**
 * Service Provider extension to EbeanServer.
 */
public interface InternalEbeanServer extends InternalEbean {

	/**
	 * Return the server name.
	 */
	public String getName();

	/**
	 * Return the associated ServerPlugin.
	 */
	public Plugin getPlugin();

	public void registerMBeans(MBeanServer mbeanServer);
	
	/**
	 * Return the AutoFetchListener.
	 */
	public AutoFetchManager getAutoFetchManager();
	
	/**
	 * Return the server cache.
	 */
	public ServerCache getServerCache();

	/**
	 * Return all the descriptors.
	 */
	public Iterator<BeanDescriptor<?>> descriptors();
	
	/**
	 * Return the BeanDescriptor for a given type of bean.
	 */
	public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> type);

	/**
	 * Return the BeanManager for a given type of bean.
	 */
	public <T> BeanManager<T> getBeanManager(Class<T> type);

	/**
	 * Return the BeanDescriptor for a database table.
	 */
	public BeanDescriptor<?> getMapBeanDescriptor(String tableName);

	/**
	 * Process committed changes from another framework.
	 * <p>
	 * This notifies this instance of the framework that beans have been
	 * committed externally to it. Either by another framework or clustered
	 * server. It uses this to maintain its cache and lucene indexes
	 * appropriately.
	 * </p>
	 */
	public void externalModification(TransactionEvent event);
	
	/**
	 * Create a ServerTransaction.
	 * <p>
	 * To specify to use the default transaction isolation use a value of -1.
	 * </p>
	 */
	public ServerTransaction createServerTransaction(boolean isExplicit, int isolationLevel);
	
	/**
	 * Return the current transaction or null if there is no current transaction.
	 */
	public ServerTransaction getCurrentServerTransaction();
	
	/**
	 * Create a ScopeTrans for a method for the given scope definition.
	 */
	public ScopeTrans createScopeTrans(TxScope txScope);

	/**
	 * Create a ServerTransaction for query purposes.
	 */
	public ServerTransaction createQueryTransaction();

	/**
	 * An event from another server in the cluster used to notify local
	 * BeanListeners of remote inserts updates and deletes.
	 */
	public void remoteListenerEvent(RemoteListenerEvent event);

	/**
	 * Create a query request object.
	 */
	public <T> OrmQueryRequest<T> createQueryRequest(Query<T> q, Transaction t);
	
	/**
	 * Compile a query.
	 */
	public <T> CQuery<T> compileQuery(Query<T> query, Transaction t);
	
	/**
	 * Return the queryEngine for this server.
	 */
	public CQueryEngine getQueryEngine();

}
