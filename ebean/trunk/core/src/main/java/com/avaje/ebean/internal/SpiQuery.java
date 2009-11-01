/**
 * Copyright (C) 2009  Robin Bygrave
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
package com.avaje.ebean.internal;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.Query;
import com.avaje.ebean.QueryListener;
import com.avaje.ebean.bean.BeanCollectionTouched;
import com.avaje.ebean.bean.CallStack;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.query.CancelableQuery;
import com.avaje.ebean.server.querydefn.OrmQueryDetail;
import com.avaje.ebean.server.querydefn.OrmQueryProperties;

/**
 * Object Relational query - Internal extension to Query object.
 */
public interface SpiQuery<T> extends Query<T> {

	public enum Mode {
		NORMAL(false),
		LAZYLOAD_MANY(false),
		LAZYLOAD_BEAN(true),
		REFRESH_BEAN(true) ;
		Mode(boolean loadContextBean){
			this.loadContextBean = loadContextBean;
		}
		private final boolean loadContextBean;
		public boolean isLoadContextBean(){
			return loadContextBean;
		}
	}
	
	/**
	 * Return true if select all properties was used to ensure the
	 * property invoking a lazy load was included in the query.
	 */
	public boolean selectAllForLazyLoadProperty();
	
	/**
	 * Set the query mode.
	 */
	public void setMode(Mode m);
	
	/**
	 * Return the query mode.
	 */
	public Mode getMode();
	
	/**
	 * Check other combinations that can make this a sharedInstance query. 
	 */
	public void deriveSharedInstance();
	
	/**
	 * This is a lazy loading query for a shared instance.
	 * That means all the beans returned by this query will
	 * also be marked as 'sharedInstance'.
	 */
	public void setSharedInstance();
	
	/**
	 * Return true if this is a lazy loading query for a shared instance.
	 */
	public boolean isSharedInstance();
	
	/**
	 * Propagate the sharedInstance or readOnly state from a parent.
	 */
	public void setParentState(int parentState);
	
	/**
	 * Return a listener that wants to be notified when the 
	 * bean collection is first used.
	 */
	public BeanCollectionTouched getBeanCollectionTouched();
	
	/**
	 * Set a listener to be notified when the bean collection has
	 * been touched (when the list/set/map is first used).
	 */
    public void setBeanCollectionTouched(BeanCollectionTouched notify);
    
	/**
	 * Set the list of Id's that is being populated.
	 * <p>
	 * This is a mutating list of id's and we are setting this so 
	 * that other threads have access to the id's before the id query
	 * has finished.
	 * </p>
	 */
	public void setIdList(List<Object> ids);
	
	/**
	 * Return the list of Id's that is currently being fetched
	 * by a background thread.
	 */
	public List<Object> getIdList();

	/**
	 * Return a copy of the query.
	 */
	public SpiQuery<T> copy();
	
	/**
	 * Set the query type (List, Set etc).
	 */
	public void setType(Type type);
	
	public String getLoadDescription();

	public void setLoadDescription(String loadDescription);

	/**
	 * Set the BeanDescriptor for the root type of this query.
	 */
	public void setBeanDescriptor(BeanDescriptor<?> desc);
	
	/**
	 * Return true if the where expressions contains a many.
	 */
	public boolean isManyInWhere();

	/**
	 * Set the query to select the id property only.
	 */
	public void setSelectId();
		
	public List<OrmQueryProperties> removeSecondaryQueries();
	
	public List<OrmQueryProperties> removeSecondaryLazyQueries();

	/**
	 * Remove any many joins from the select. Joins to Manys may still
	 * be required to support the where or order by clauses and in this 
	 * case typically distinct must be used.
	 */
	public void removeManyJoins();

	/**
	 * Return the TransactionContext.
	 * <p>
	 * If no TransactionContext is present on the query then the TransactionContext
	 * from the Transaction is used (transaction scoped persistence context).
	 * </p>
	 */
	public PersistenceContext getPersistenceContext();
	
	/**
	 * Set an explicit TransactionContext (typically for a refresh query).
	 * <p>
	 * If no TransactionContext is present on the query then the TransactionContext
	 * from the Transaction is used (transaction scoped persistence context).
	 * </p>
	 */
	public void setPersistenceContext(PersistenceContext transactionContext);
	
	/**
	 * Return true if the query detail has neither select or joins specified.
	 */
	public boolean isDetailEmpty();

	/**
	 * Return explicit autoFetch setting or null. If null then not explicitly
	 * set so we use the default behaviour.
	 */
	public Boolean isAutofetch();

	/**
	 * If return null then no autoFetch profiling for this query. If a
	 * AutoFetchManager is returned this implies that profiling is turned on for
	 * this query (and all the objects this query creates).
	 */
	public AutoFetchManager getAutoFetchManager();

	/**
	 * This has the effect of turning on autoFetch profiling for this query.
	 */
	public void setAutoFetchManager(AutoFetchManager manager);

	/**
	 * Return the origin point for the query.
	 * <p>
	 * This MUST be call prior to a query being changed via tuning. This is
	 * because the queryPlanHash is used to identify the query point.
	 * </p>
	 */
	public ObjectGraphNode setOrigin(CallStack callStack);
	
	/**
	 * Set the profile point of the bean or collection that is lazy loading.
	 * <p>
	 * This enables use to hook this back to the original 'root' query by the
	 * queryPlanHash and stackPoint.
	 * </p>
	 */
	public void setParentNode(ObjectGraphNode node);

	/**
	 * Set the property that invoked the lazy load and MUST be included in the 
	 * lazy loading query.
	 */
	public void setLazyLoadProperty(String lazyLoadProperty);
	
	/**
	 * Used to hook back a lazy loading query to the original query (query
	 * point).
	 * <p>
	 * This will return null or an "original" query.
	 * </p>
	 */
	public ObjectGraphNode getParentNode();

	/**
	 * Return false when this is a lazy load or refresh query for a bean.
	 * <p>
	 * We just take/copy the data from those beans and don't collect autoFetch
	 * usage profiling on those lazy load or refresh beans. 
	 * </p>
	 */
	public boolean isUsageProfiling();

	/**
	 * Set to false if this query should not be included in the autoFetch usage
	 * profiling information.
	 */
	public void setUsageProfiling(boolean usageProfiling);

	/**
	 * Return the query name.
	 */
	public String getName();

	/**
	 * Calculate a hash used by AutoFetch to identify when a query has changed 
	 * (and hence potentially needs a new tuned query plan to be developed).
	 * <p>
	 * Excludes bind values and occurs prior to AutoFetch potentially tuning/modifying the query.
	 * </p>
	 */
	public int queryAutofetchHash();

	/**
	 * Identifies queries that are the same bar the bind variables.
	 * <p>
	 * This is used AFTER AutoFetch has potentially tuned the query. This is used to identify
	 * and reused query plans (the final SQL string and associated SqlTree object).
	 * </p>
	 * <p>
	 * Excludes the actual bind values (as they don't effect the query plan).
	 * </p>
	 */
	public int queryPlanHash(BeanQueryRequest<?> request);
	
	/**
	 * Calculate a hash based on the bind values used in the query.
	 * <p>
	 * Combined with queryPlanHash() to return getQueryHash (a unique hash for a query).
	 * </p>
	 */
	public int queryBindHash();
		
	/**
	 * Identifies queries that are exactly the same including bind variables.
	 */
	public int queryHash();
	
	/**
	 * Return true if this is a query based on a SqlSelect rather than
	 * generated.
	 */
	public boolean isSqlSelect();

	/**
	 * Return the Order By clause or null if there is none defined.
	 */
	public String getOrderByStringFormat();
	
	/**
	 * Return additional where clause. This should be added to any where clause
	 * that was part of the original query.
	 */
	public String getAdditionalWhere();

	/**
	 * Can return null if no expressions where added to the where clause.
	 */
	public SpiExpressionList<T> getWhereExpressions();

	/**
	 * Can return null if no expressions where added to the having clause.
	 */
	public SpiExpressionList<T> getHavingExpressions();

	/**
	 * Return additional having clause. Where raw String expressions are added
	 * to having clause rather than Expression objects.
	 */
	public String getAdditionalHaving();

	/**
	 * Returns true if either firstRow or maxRows has been set.
	 */
	public boolean hasMaxRowsOrFirstRow();

	/**
	 * Return true if this query should use/check the bean cache.
	 */
	public Boolean isUseBeanCache();

	/**
	 * Return true if this query should use/check the query cache.
	 */
	public boolean isUseQueryCache();

	/**
	 * Return true if the beans from this query should be loaded
	 * into the bean cache.
	 */
	public boolean isLoadBeanCache();
	
	/**
	 * Return true if the beans returned by this query should be
	 * read only.
	 */
	public Boolean isReadOnly();
	
	/**
	 * Adds this bean to the persistence context prior to executing the query.
	 */
	public void contextAdd(EntityBean bean);

	/**
	 * Return the type of beans queries.
	 */
	public Class<T> getBeanType();

	/**
	 * Return the query timeout.
	 */
	public int getTimeout();

	/**
	 * Return the objects that should be added to the persistence context prior
	 * to executing the query.
	 */
	public ArrayList<EntityBean> getContextAdditions();

	/**
	 * Return the bind parameters.
	 */
	public BindParams getBindParams();

	/**
	 * Get the orm query as a String. Only available if the query was built from
	 * a string.
	 */
	public String getQuery();

	/**
	 * Replace the query detail. This is used by the autoFetch feature to as a
	 * fast way to set the query properties and joins.
	 * <p>
	 * Note care must be taken to keep the where, orderBy, firstRows and maxRows
	 * held in the detail attributes.
	 * </p>
	 */
	public void setDetail(OrmQueryDetail detail);

	/**
	 * Set to true if this query has been tuned by autoFetch.
	 */
	public void setAutoFetchTuned(boolean autoFetchTuned);
	
	/**
	 * Return the query detail.
	 */
	public OrmQueryDetail getDetail();

	public TableJoin getIncludeTableJoin();

	public void setIncludeTableJoin(TableJoin includeTableJoin);

	/**
	 * Return the property used to specify keys for a map.
	 */
	public String getMapKey();

	/**
	 * Return the initial capacity for List Set or Maps.
	 */
	public int getInitialCapacity();

	/**
	 * Return the number of rows after which fetching should occur in a
	 * background thread.
	 */
	public int getBackgroundFetchAfter();

	/**
	 * Return the maximum number of rows to return in the query.
	 */
	public int getMaxRows();

	/**
	 * Return the index of the first row to return in the query.
	 */
	public int getFirstRow();

	/**
	 * return true if this query uses DISTINCT.
	 */
	public boolean isDistinct();

	/**
	 * Set default select clauses where none have been explicitly defined.
	 */
	public void setDefaultSelectClause();

	/**
	 * Return the where clause from a parsed string query.
	 */
	public String getRawWhereClause();

	/**
	 * Return the Id value.
	 */
	public Object getId();

	/**
	 * Return the queryListener.
	 */
	public QueryListener<T> getListener();

	/**
	 * Return true if this query should use its own transaction.
	 * <p>
	 * This is true for background fetching and when using QueryListener.
	 * </p>
	 */
	public boolean createOwnTransaction();

	/**
	 * Set the generated sql for debug purposes.
	 * 
	 * @param generatedSql
	 */
	public void setGeneratedSql(String generatedSql);
	
	/**
	 * Return the hint for Statement.setFetchSize().
	 */
	public int getBufferFetchSizeHint();
	
	/**
	 * Return true if this is a query executing in the background.
	 */
	public boolean isFutureFetch();

	/**
	 * Set to true to indicate the query is executing in a background
	 * thread asynchronously.
	 */
	public void setFutureFetch(boolean futureFetch);

	/**
	 * Set the underlying cancelable query (with the PreparedStatement). 
	 */
	public void setCancelableQuery(CancelableQuery cancelableQuery);
		
	/**
	 * Return true if this query has been cancelled.
	 */
	public boolean isCancelled();
}
