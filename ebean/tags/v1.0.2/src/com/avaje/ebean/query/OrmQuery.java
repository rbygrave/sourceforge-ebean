package com.avaje.ebean.query;

import java.util.ArrayList;

import com.avaje.ebean.Query;
import com.avaje.ebean.QueryListener;
import com.avaje.ebean.bean.CallStack;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.ObjectGraphOrigin;
import com.avaje.ebean.expression.InternalExpressionList;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.core.TransactionContext;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.util.BindParams;

/**
 * Object Relational query - Internal extension to Query object.
 */
public interface OrmQuery<T> extends Query<T> {

	/**
	 * Return the TransactionContext.
	 * <p>
	 * If no TransactionContext is present on the query then the TransactionContext
	 * from the Transaction is used (transaction scoped persistence context).
	 * </p>
	 */
	public TransactionContext getTransactionContext();
	
	/**
	 * Set an explicit TransactionContext (typically for a refresh query).
	 * <p>
	 * If no TransactionContext is present on the query then the TransactionContext
	 * from the Transaction is used (transaction scoped persistence context).
	 * </p>
	 */
	public void setTransactionContext(TransactionContext transactionContext);
	
	/**
	 * Return true if the query detail has neither select or joins specified.
	 */
	public boolean isDetailEmpty();

	/**
	 * Return explicit autoFetch setting or null. If null then not explicitly
	 * set so we use the default behaviour.
	 */
	public Boolean isAutoFetch();

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
	 * Set the profile point of the bean or collection that is lazy loading.
	 * <p>
	 * This enables use to hook this back to the original 'root' query by the
	 * queryPlanHash and stackPoint.
	 * </p>
	 */
	public void setParentNode(ObjectGraphNode node);

	/**
	 * Return the origin point for the query.
	 * <p>
	 * This MUST be call prior to a query being changed via tuning. This is
	 * because the queryPlanHash is used to identify the query point.
	 * </p>
	 */
	public ObjectGraphOrigin createObjectGraphOrigin(CallStack callStack);

	/**
	 * Returns the origin query point.
	 * <p>
	 * This is the query point of the original query that builds the object
	 * graph.
	 * </p>
	 */
	public ObjectGraphOrigin getObjectGraphOrigin();

	/**
	 * Used to hook back a lazy loading query to the original query (query
	 * point).
	 * <p>
	 * This will return null or an "original" query.
	 * </p>
	 */
	public ObjectGraphNode getParentNode();

	/**
	 * Create a new AutoFetchNode for a given beanIndex and path.
	 * <p>
	 * This effectively identifies a single point in an object graph.
	 * </p>
	 */
	public ObjectGraphNode createObjectGraphNode(String beanIndex, String path);

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
	 * Identifies queries that are exactly the same including bind variables.
	 */
	public int getQueryHash();

	/**
	 * Identifies queries that are the same bar the bind variables.
	 */
	public int getQueryPlanHash();

	/**
	 * Return true if this is a query based on a SqlSelect rather than
	 * generated.
	 */
	public boolean isSqlSelect();

	/**
	 * Return additional where clause. This should be added to any where clause
	 * that was part of the original query.
	 */
	public String getAdditionalWhere();

	/**
	 * Can return null if no expressions where added to the where clause.
	 */
	public InternalExpressionList<T> getWhereExpressions();

	/**
	 * Can return null if no expressions where added to the having clause.
	 */
	public InternalExpressionList<T> getHavingExpressions();

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
	 * Return true if this query should use/check the cache.
	 */
	public boolean isUseCache();

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
	 * Return the order by clause.
	 */
	public String getOrderBy();

	/**
	 * Return the where clause.
	 */
	public String getWhere();

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
	public boolean useOwnTransaction();

	/**
	 * Set the generated sql for debug purposes.
	 * 
	 * @param generatedSql
	 */
	public void setGeneratedSql(String generatedSql);
}
