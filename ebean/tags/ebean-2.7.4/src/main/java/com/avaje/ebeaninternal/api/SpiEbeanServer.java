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
package com.avaje.ebeaninternal.api;

import java.util.List;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.TxScope;
import com.avaje.ebean.bean.BeanCollectionLoader;
import com.avaje.ebean.bean.BeanLoader;
import com.avaje.ebean.bean.CallStack;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebeaninternal.server.autofetch.AutoFetchManager;
import com.avaje.ebeaninternal.server.core.PstmtBatch;
import com.avaje.ebeaninternal.server.core.SpiOrmQueryRequest;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.lucene.LuceneIndexManager;
import com.avaje.ebeaninternal.server.query.CQuery;
import com.avaje.ebeaninternal.server.query.CQueryEngine;
import com.avaje.ebeaninternal.server.transaction.RemoteTransactionEvent;

/**
 * Service Provider extension to EbeanServer.
 */
public interface SpiEbeanServer extends EbeanServer, BeanLoader, BeanCollectionLoader {

	/**
	 * Return true if DeleteMissingChildren defaults to true for stateless updates.
	 */
    public boolean isDefaultDeleteMissingChildren();

	/**
	 * Return true if UpdateNullProperties defaults to true for stateless updates.
	 */
	public boolean isDefaultUpdateNullProperties();

    /**
     * Return true if vanilla beans should be returned by queries by default.
     */
    public boolean isVanillaMode();
    
	/**
	 * Return the DatabasePlatform for this server.
	 */
	public DatabasePlatform getDatabasePlatform();
	
	/**
	 * Return a JDBC driver specific handler for batching.
	 * <p>
	 * Required for Oracle specific batch handling.
	 * </p>
	 */
	public PstmtBatch getPstmtBatch();
	
	/**
	 * Create an object to represent the current CallStack.
	 * <p>
	 * Typically used to identify the origin of queries for Autofetch
	 * and object graph costing.
	 * </p>
	 */
	public CallStack createCallStack();
	
	/**
	 * Return the DDL generator.
	 */
	public DdlGenerator getDdlGenerator();

	/**
	 * Return the AutoFetchListener.
	 */
	public AutoFetchManager getAutoFetchManager();
	
	/**
	 * Return the LuceneIndexManager
	 */
    public LuceneIndexManager getLuceneIndexManager();


	/**
	 * Clear the query execution statistics.
	 */
	public void clearQueryStatistics();

	/**
	 * Return all the descriptors.
	 */
	public List<BeanDescriptor<?>> getBeanDescriptors();

	/**
	 * Return the BeanDescriptor for a given type of bean.
	 */
	public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> type);

    /**
     * Return BeanDescriptor using it's unique id.
     */
    public BeanDescriptor<?> getBeanDescriptorById(String descriptorId);

    /**
     * Return BeanDescriptors mapped to this table.
     */
    public List<BeanDescriptor<?>> getBeanDescriptors(String tableName);

	/**
	 * Process committed changes from another framework.
	 * <p>
	 * This notifies this instance of the framework that beans have been
	 * committed externally to it. Either by another framework or clustered
	 * server. It uses this to maintain its cache and lucene indexes
	 * appropriately.
	 * </p>
	 */
	public void externalModification(TransactionEventTable event);

	/**
	 * Create a ServerTransaction.
	 * <p>
	 * To specify to use the default transaction isolation use a value of -1.
	 * </p>
	 */
	public SpiTransaction createServerTransaction(boolean isExplicit, int isolationLevel);

	/**
	 * Return the current transaction or null if there is no current
	 * transaction.
	 */
	public SpiTransaction getCurrentServerTransaction();

	/**
	 * Create a ScopeTrans for a method for the given scope definition.
	 */
	public ScopeTrans createScopeTrans(TxScope txScope);

	/**
	 * Create a ServerTransaction for query purposes.
	 */
	public SpiTransaction createQueryTransaction();

	/**
	 * An event from another server in the cluster used to notify local
	 * BeanListeners of remote inserts updates and deletes.
	 */
    public void remoteTransactionEvent(RemoteTransactionEvent event);

	
	/**
	 * Create a query request object.
	 */
	public <T> SpiOrmQueryRequest<T> createQueryRequest(BeanDescriptor<T> desc, SpiQuery<T> q, Transaction t);

	/**
	 * Compile a query.
	 */
	public <T> CQuery<T> compileQuery(Query<T> query, Transaction t);

	/**
	 * Return the queryEngine for this server.
	 */
	public CQueryEngine getQueryEngine();

	/**
	 * Execute the findId's query but without copying the query.
	 * <p>
	 * Used so that the list of Id's can be made accessible to client code
	 * before the query has finished (if executing in a background thread).
	 * </p>
	 */
	public <T> List<Object> findIdsWithCopy(Query<T> query, Transaction t);

	/**
	 * Execute the findRowCount query but without copying the query.
	 */
    public <T> int findRowCountWithCopy(Query<T> query, Transaction t);

	/**
	 * Load a batch of Associated One Beans.
	 */
	public void loadBean(LoadBeanRequest loadRequest);

	/**
	 * Lazy load a batch of Many's.
	 */
	public void loadMany(LoadManyRequest loadRequest);

	/**
	 * Return the default batch size for lazy loading.
	 */
	public int getLazyLoadBatchSize();
	
    /**
     * Return true if the type is known as an Entity or Xml type
     * or a List Set or Map of known bean types.
     */
    public boolean isSupportedType(java.lang.reflect.Type genericType);

}
