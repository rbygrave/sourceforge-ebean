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

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.BeanQueryRequest;
import com.avaje.ebean.bean.QueryType;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.cache.Cache;
import com.avaje.ebean.server.cache.CacheManager;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.ManyType;
import com.avaje.ebean.server.deploy.jointree.JoinTree;
import com.avaje.ebean.server.query.CQueryPlan;

/**
 * Wraps the objects involved in executing a Query.
 */
public final class OrmQueryRequest<T> extends BeanRequest implements BeanQueryRequest<T> {
	
	/**
	 * The associated BeanDescriptor.
	 */
	private final BeanManager<T> beanManager;

	private final BeanDescriptor<T> beanDescriptor;
	
	private final OrmQueryEngine queryEngine;

	private final OrmQuery<T> query;

	private final BeanFinder<T> finder;

	private TransactionContext transactionContext;

	private boolean createdTransaction;

	private ManyType manyType;

	private Cache beanQueryCache;

	private Integer cacheKey;

	private int queryPlanHash;

	/**
	 * Flag set if background fetching taking place. In this case the
	 * transaction is rolled back by the background fetching thread.
	 * Background fetching always takes place in its own transaction.
	 */
	private boolean backgroundFetching;
	
	/**
	 * Create the InternalQueryRequest.
	 */
	public OrmQueryRequest(InternalEbeanServer server, OrmQueryEngine queryEngine, OrmQuery<T> query,
			BeanManager<T> mgr, ServerTransaction t) {

		super(server, t);
		
		this.beanManager = mgr;
		this.beanDescriptor = mgr.getBeanDescriptor();
		this.finder = beanDescriptor.getBeanFinder();
		this.queryEngine = queryEngine;
		this.query = query;
	}
	
	public BeanManager<T> getBeanManager() {
		return beanManager;
	}

	/**
	 * Return the BeanDescriptor for the associated bean.
	 */
	public BeanDescriptor<T> getBeanDescriptor() {
		return beanDescriptor;
	}
	
	/**
	 * Calculate the query plan hash AFTER any potential AutoFetch tuning.
	 */
	public void calculateQueryPlanHash() {
		this.queryPlanHash = query.queryPlanHash(this);
	}
	
	/**
	 * Return true if this is a query using generated sql. If false this query
	 * will use raw sql (Entity bean based on raw sql select).
	 */
	public boolean isSqlSelect() {
		return query.isSqlSelect();
	}

	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

//	/**
//	 * Rollback the local transaction if required.
//	 */
//	public void rollbackTransIfRequired(String stackTrace) {
//		if (createdTransaction) {
//			transaction.rollback();
//		}
//	}

	/**
	 * This will create a local (readOnly) transaction if no current transaction
	 * exists.
	 * <p>
	 * A transaction may have been passed in explicitly or currently be active
	 * in the thread local. If not, then a readOnly transaction is created to
	 * execute this query.
	 * </p>
	 */
	public void initTransIfRequired() {
		if (transaction == null) {
			// get current transaction
			transaction = ebeanServer.getCurrentServerTransaction();
			if (transaction == null || !transaction.isActive() || query.useOwnTransaction()) {
				// create an implicit transaction to execute this query
				transaction = ebeanServer.createQueryTransaction();

				// leaving off setReadOnly(true) for performance reasons
				// TODO: review performance of transaction.setReadOnly(true);
				// trans.setReadOnly(true);
				createdTransaction = true;
			}
		}
		this.transactionContext = determineTransactionContext(query, transaction);
	}

	/**
	 * Get the TransactionContext either explicitly set on the query or transaction scoped.
	 */
	private TransactionContext determineTransactionContext(OrmQuery<?> query, ServerTransaction t){
		
		TransactionContext ctx = query.getTransactionContext();
		if (ctx == null){
			ctx = t.getTransactionContext();
		}
		return ctx;
	}
	
	/**
	 * Will end a locally created transaction.
	 * <p>
	 * It ends the transaction by using a rollback() as the transaction is known
	 * to be readOnly.
	 * </p>
	 */
	public void endTransIfRequired() {
		if (createdTransaction && !backgroundFetching) {
			// we can rollback as readOnly transaction
			transaction.rollback();
		}
	}

	/**
	 * This query is using background fetching.
	 */
	public void setBackgroundFetching() {
		backgroundFetching = true;
	}

	/**
	 * Return true if this is a find by id (rather than List Set or Map).
	 */
	public boolean isFindById() {
		return manyType == ManyType.FIND_ONE;
	}

	/**
	 * Return true if this is a subquery (as part of InQueryExpression).
	 */
	public boolean isSubQuery() {
		return manyType == null;
	}
	
	/**
	 * Execute the query as findById.
	 */
	public Object findId() {
		manyType = ManyType.FIND_ONE;
		return queryEngine.findId(this);
	}

	/**
	 * Execute the query as findList.
	 */
	public List<?> findList() {
		manyType = ManyType.LIST;
		return (List<?>) queryEngine.findMany(this);
	}

	/**
	 * Execute the query as findSet.
	 */
	public Set<?> findSet() {
		manyType = ManyType.SET;
		return (Set<?>) queryEngine.findMany(this);
	}

	/**
	 * Execute the query as findMap.
	 */
	public Map<?, ?> findMap() {
		manyType = ManyType.MAP;
		return (Map<?, ?>) queryEngine.findMany(this);
	}
	
	

	public QueryType getQueryType() {
		if (manyType != null){
			return manyType.getQueryType();
		} else {
			return null;
		}
	}

	/**
	 * Return the 'Join Tree' for this request.
	 */
	public JoinTree getBeanJoinTree() {
		return beanManager.getBeanJoinTree();
	}

	/**
	 * Return a bean specific finder if one has been set.
	 */
	public BeanFinder<T> getBeanFinder() {
		return finder;
	}

	/**
	 * Return the find that is to be performed.
	 */
	public OrmQuery<T> getQuery() {
		return query;
	}

	/**
	 * Return the many property that is fetched in the query or null if there is
	 * not one.
	 */
	public BeanPropertyAssocMany<?> getManyProperty() {
		return beanDescriptor.getManyProperty(query);
	}

	/**
	 * Return a queryPlan for the current query if one exists. Returns null if
	 * no query plan for this query exists.
	 */
	public CQueryPlan getQueryPlan() {
		return beanDescriptor.getQueryPlan(queryPlanHash);
	}

	/**
	 * Return the queryPlanHash.
	 * <p>
	 * This identifies the query plan for a given bean type. It effectively
	 * matches a SQL statement with ? bind variables. A query plan can be reused
	 * with just the bind variables changing.
	 * </p>
	 */
	public int getQueryPlanHash() {
		return queryPlanHash;
	}

	/**
	 * Put the QueryPlan into the cache.
	 */
	public void putQueryPlan(CQueryPlan queryPlan) {
		beanDescriptor.putQueryPlan(queryPlanHash, queryPlan);
	}

	/**
	 * Return the type (List, Set or Map) that this fetch returns.
	 */
	public ManyType getManyType() {
		return manyType;
	}

	/**
	 * Try to get the query result from cache.
	 */
	public Object getFromCache(CacheManager serverCache) {

		if (query.isUseCache()) {
			beanQueryCache = serverCache.getQueryCache(beanDescriptor.getBeanType());

			if (manyType == null) {
				// the query plan and bind values must be the same
				cacheKey = Integer.valueOf(query.queryHash());

			} else {
				// additionally the return type (List/Set/Map) must be the same
				cacheKey = Integer.valueOf(31 * query.queryHash() + manyType.hashCode());
			}

			return beanQueryCache.get(cacheKey);
		}
		return null;
	}

	public void putToCacheOne(Object queryResult) {
		beanQueryCache.put(cacheKey, queryResult);
	}

	public void putToCacheMany(Object queryResult) {
		beanQueryCache.put(cacheKey, queryResult);
	}
}
