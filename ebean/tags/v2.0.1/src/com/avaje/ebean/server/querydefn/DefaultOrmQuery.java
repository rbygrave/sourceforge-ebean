package com.avaje.ebean.server.querydefn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.PersistenceException;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Expression;
import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.QueryListener;
import com.avaje.ebean.bean.CallStack;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.ObjectGraphOrigin;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.BindParams;
import com.avaje.ebean.internal.SpiExpressionList;
import com.avaje.ebean.internal.PersistenceContext;
import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.DeployNamedQuery;
import com.avaje.ebean.server.deploy.RawSqlSelect;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.util.DefaultExpressionList;

/**
 * Default implementation of an Object Relational query.
 */
public final class DefaultOrmQuery<T> implements SpiQuery<T> {

	private static final long serialVersionUID = 6838006264714672460L;

	private final Class<T> beanType;
	
	private transient final EbeanServer server;
	
	private transient final ExpressionFactory expressionFactory;
	
	/**
	 * Used to add beans to the PersistanceContext prior to query.
	 */
	private transient ArrayList<EntityBean> contextAdditions;
	
	private transient QueryListener<T> queryListener;

	/**
	 * For lazy loading of ManyToMany we need to add a join to the intersection
	 * table. This is that join to the intersection table.
	 */
	private transient TableJoin includeTableJoin;

	private transient AutoFetchManager autoFetchManager;

	private transient BeanDescriptor<T> beanDescriptor;
	
	/**
	 * The name of the query.
	 */
	private String name;
	
	private Type type;
	
	/**
	 * Holds query in structured form.
	 */
	private OrmQueryDetail detail;

	private OrmQueryAttributes attributes;

	private String generatedSql;

	/**
	 * Query language version of the query.
	 */
	private String query;

	private String additionalWhere;

	private String additionalHaving;

	/**
	 * Set to true if you want a DISTINCT query.
	 */
	private boolean distinct;

	/**
	 * The rows after which the fetch continues in a bg thread.
	 */
	private int backgroundFetchAfter;

	/**
	 * Used to increase the initial capacity of the list set or map being
	 * fetched. Useful if fetching a large amount of data into a Map or Set to
	 * reduce rehashing.
	 */
	private int initialCapacity;

	private int timeout = -1;
	
	/**
	 * The property used to get the key value for a Map.
	 */
	private String mapKey;

	/**
	 * Used for find by id type query.
	 */
	private Object id;

	/**
	 * Bind parameters when using the query language.
	 */
	private BindParams bindParams;

	private DefaultExpressionList<T> whereExpressions;

	private DefaultExpressionList<T> havingExpressions;

	private boolean usageProfiling = true;

	private boolean useCache;

	private boolean sqlSelect;

	/**
	 * Allow for explicit on off or null for default.
	 */
	private Boolean autoFetch;
	
	/**
	 * Set to true if this query has been tuned by autoFetch.
	 */
	private boolean autoFetchTuned;
	
	/**
	 * The node of the bean or collection that fired lazy loading. Not null if
	 * profiling is on and this query is for lazy loading. Used to hook back a
	 * lazy loading query to the "original" query point.
	 */
	private ObjectGraphNode parentNode;

	private ObjectGraphOrigin objectGraphOrigin;

	/**
	 * Hash of final query after AutoFetch tuning.
	 */
	private int queryPlanHash;
	
	private transient PersistenceContext persistenceContext;

	public DefaultOrmQuery(Class<T> beanType, EbeanServer server) {
		this.beanType = beanType;
		this.server = server;
		this.expressionFactory = server.getExpressionFactory();
		this.detail = new OrmQueryDetail();
		this.attributes = new OrmQueryAttributes();
		this.name = "";
	}

	/**
	 * Additional supply a query which is parsed.
	 */
	public DefaultOrmQuery(Class<T> beanType, EbeanServer server, DeployNamedQuery namedQuery)
			throws PersistenceException {

		this.beanType = beanType;
		this.server = server;
		this.expressionFactory = server.getExpressionFactory();
		this.name = namedQuery.getName();
		this.sqlSelect = namedQuery.isSqlSelect();
		if (sqlSelect) {
			this.detail = new OrmQueryDetail();
			this.attributes = new OrmQueryAttributes();
			// potentially with where and having clause...
			RawSqlSelect sqlSelect = namedQuery.getSqlSelect();
			additionalWhere = sqlSelect.getWhereClause();
			additionalHaving = sqlSelect.getHavingClause();
		} else {
			// parse the entire query...
			setQuery(namedQuery.getQuery());
		}
	}
	
	/**
	 * Set the BeanDescriptor for the root type of this query.
	 */
	public void setBeanDescriptor(BeanDescriptor<T> beanDescriptor) {
		this.beanDescriptor = beanDescriptor;
	}
	
	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	/**
	 * Return true if the where expressions contains a many property.
	 */
	public boolean isManyInWhere() {
		
    	if (whereExpressions != null){
    		return whereExpressions.containsMany(beanDescriptor);
    	} else {
    		return false;
    	}
	}

	/**
	 * Set the select clause to select the Id property.
	 */
	public void setSelectId() {
		// clear select and fetch joins..
		detail.clear();
		
		select(beanDescriptor.getIdBinder().getIdProperty());
	}
	
	public DefaultOrmQuery<T> copy() {
		// Not including these in the copy:
		// ArrayList<EntityBean> contextAdditions;
		// QueryListener<T> queryListener;
		// TransactionContext transactionContext;

		DefaultOrmQuery<T> copy = new DefaultOrmQuery<T>(beanType, server);
		copy.name = name;
		copy.includeTableJoin = includeTableJoin;
		copy.autoFetchManager = autoFetchManager;
		//copy.generatedSql = generatedSql;
		copy.query = query;
		copy.additionalWhere = additionalWhere;
		copy.additionalHaving = additionalHaving;
		copy.distinct = distinct;
		copy.backgroundFetchAfter = backgroundFetchAfter;
		copy.initialCapacity = initialCapacity;
		copy.timeout = timeout;
		copy.mapKey = mapKey;
		copy.id = id;
		copy.useCache = useCache;
		copy.sqlSelect = sqlSelect;
		if (detail != null){
			copy.detail = detail.copy();
		}
		if (attributes != null){
			copy.attributes = attributes.copy();
		}
		if (bindParams != null){
			copy.bindParams = bindParams.copy();
		}
		if (whereExpressions != null){
			copy.whereExpressions = whereExpressions.copy(copy);
		}
		if (havingExpressions != null){
			copy.havingExpressions = havingExpressions.copy(copy);
		}
		copy.usageProfiling = usageProfiling;
		copy.autoFetch = autoFetch;
		copy.parentNode = parentNode;
		copy.objectGraphOrigin = objectGraphOrigin;
		//copy.autoFetchTuned = autoFetchTuned;
		//copy.autoFetchQueryPlanHash = autoFetchQueryPlanHash;
		
		return copy;
	}
	

	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * Return the TransactionContext.
	 * <p>
	 * If no TransactionContext is present on the query then the TransactionContext
	 * from the Transaction is used (transaction scoped persistence context).
	 * </p>
	 */
	public PersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

	/**
	 * Set an explicit TransactionContext (typically for a refresh query).
	 * <p>
	 * If no TransactionContext is present on the query then the TransactionContext
	 * from the Transaction is used (transaction scoped persistence context).
	 * </p>
	 */
	public void setPersistenceContext(PersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	/**
	 * Return true if the query detail has neither select or joins specified.
	 */
	public boolean isDetailEmpty() {
		return detail.isEmpty();
	}

	public boolean isAutofetchTuned() {
		return autoFetchTuned;
	}

	public void setAutoFetchTuned(boolean autoFetchTuned) {
		this.autoFetchTuned = autoFetchTuned;
	}

	public Boolean isAutofetch() {
		return sqlSelect ? Boolean.FALSE : autoFetch;
	}

	public DefaultOrmQuery<T> setAutoFetch(boolean autoFetch) {
		return setAutofetch(autoFetch);
	}
	
	public DefaultOrmQuery<T> setAutofetch(boolean autoFetch) {
		this.autoFetch = autoFetch;
		return this;
	}
	
	public AutoFetchManager getAutoFetchManager() {
		return autoFetchManager;
	}

	public void setAutoFetchManager(AutoFetchManager autoFetchManager) {
		this.autoFetchManager = autoFetchManager;
	}

	public boolean isUsageProfiling() {
		return usageProfiling;
	}

	public void setUsageProfiling(boolean usageProfiling) {
		this.usageProfiling = usageProfiling;
	}

	public void setParentNode(ObjectGraphNode parentNode) {
		this.parentNode = parentNode;
		this.objectGraphOrigin = parentNode.getOriginQueryPoint();
	}

	public ObjectGraphNode getParentNode() {
		return parentNode;
	}

	public ObjectGraphOrigin getObjectGraphOrigin(){
		return objectGraphOrigin;
	}
	
	public ObjectGraphOrigin createObjectGraphOrigin(CallStack callStack) {

		// calculate base query hash prior to it being tuned
		objectGraphOrigin = new ObjectGraphOrigin(queryAutofetchHash(), callStack, beanType.getName());
		return objectGraphOrigin;
	}

	public ObjectGraphNode createObjectGraphNode(String beanIndex, String path) {
		if (parentNode != null) {
			return new ObjectGraphNode(parentNode, beanIndex, path);
		} else {
			return new ObjectGraphNode(objectGraphOrigin, beanIndex, path);
		}
	}

	
	/**
	 * Calculate the query hash for either AutoFetch query tuning or Query Plan caching.
	 */
	private int calculateHash(BeanQueryRequest<?> request) {

		// exclude bind values and things unrelated to
		// the sql being generated

		// must use String name of class as actual class hashCode
		// can change between JVM restarts.
		int hc = beanType.getName().hashCode();

		hc = hc * 31 + (autoFetchTuned ? 31 : 0);
		hc = hc * 31 + (distinct ? 31 : 0);

		hc = hc * 31 + attributes.queryPlanHash();
		hc = hc * 31 + detail.queryPlanHash();
		hc = hc * 31 + (query == null ? 0 : query.hashCode());

		hc = hc * 31 + (additionalWhere == null ? 0 : additionalWhere.hashCode());
		hc = hc * 31 + (additionalHaving == null ? 0 : additionalHaving.hashCode());
		hc = hc * 31 + (mapKey == null ? 0 : mapKey.hashCode());
		hc = hc * 31 + (id == null ? 0 : 1);

		if (request == null){
			// for AutoFetch...
			hc = hc * 31 + (whereExpressions == null ? 0 : whereExpressions.queryAutoFetchHash());
			hc = hc * 31 + (havingExpressions == null ? 0 : havingExpressions.queryAutoFetchHash());
			
		} else {
			// for query plan...
			hc = hc * 31 + (whereExpressions == null ? 0 : whereExpressions.queryPlanHash(request));
			hc = hc * 31 + (havingExpressions == null ? 0 : havingExpressions.queryPlanHash(request));
		}
		
		return hc;
	}
	
	/**
	 * Calculate a hash used by AutoFetch to identify when a query has changed 
	 * (and hence potentially needs a new tuned query plan to be developed).
	 */
	public int queryAutofetchHash() {
		
		return calculateHash(null);
	}
	
	/**
	 * Calculate a hash that should be unique for the generated SQL across a
	 * given bean type.
	 * <p>
	 * This can used to enable the caching and reuse of a 'query plan'.
	 * </p>
	 * <p>
	 * This is calculated AFTER AutoFetch query tuning has occurred.
	 * </p>
	 */
	public int queryPlanHash(BeanQueryRequest<?> request) {

		queryPlanHash = calculateHash(request);
		return queryPlanHash;
	}

	/**
	 * Calculate a hash based on the bind values used in the query.
	 * <p>
	 * Used with queryPlanHash() to get a unique hash for a query.
	 * </p>
	 */
	public int queryBindHash() {
		int hc = (id == null ? 0 : id.hashCode());
		hc = hc * 31 + (whereExpressions == null ? 0 : whereExpressions.queryBindHash());
		hc = hc * 31 + (havingExpressions == null ? 0 : havingExpressions.queryBindHash());
		hc = hc * 31 + (bindParams == null ? 0 : bindParams.queryBindHash());
		hc = hc * 31 + (contextAdditions == null ? 0 : contextAdditions.hashCode());

		return hc;
	}

	/**
	 * Return a hash that includes the query plan and bind values.
	 * <p>
	 * This hash can be used to identify if we have executed the exact same query 
	 * (including bind values) before.
	 * </p>
	 */
	public int queryHash() {
		// calculateQueryPlanHash is called just after potential AutoFetch tuning
		// so queryPlanHash is calculated well before this method is called
		int hc = queryPlanHash;
		hc = hc * 31 + queryBindHash();
		return hc;
	}

	/**
	 * Return the query name.
	 */
	public String getName() {
		return name;
	}

	public boolean isSqlSelect() {
		return sqlSelect;
	}

	/**
	 * Return any additional where clauses.
	 */
	public String getAdditionalWhere() {
		return additionalWhere;
	}

	/**
	 * Return the timeout.
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Return any additional having clauses.
	 */
	public String getAdditionalHaving() {
		return additionalHaving;
	}

	public boolean hasMaxRowsOrFirstRow() {
		return attributes.hasMaxRowsOrFirstRow();
	}

	public boolean isUseCache() {
		return useCache;
	}

	public DefaultOrmQuery<T> setUseCache(boolean useCache) {
		this.useCache = useCache;
		return this;
	}

	public DefaultOrmQuery<T> setTimeout(int secs) {
		this.timeout = secs;
		return this;
	}

	public DefaultOrmQuery<T> setQuery(String query) throws PersistenceException {

		this.query = query;

		OrmQueryDetailParser parser = new OrmQueryDetailParser(query);
		parser.parse();

		detail = parser.getDetail();
		attributes = parser.getAttributes();
		return this;
	}

	public DefaultOrmQuery<T> setProperties(String columns) {
		return select(columns);
	}

	public DefaultOrmQuery<T> select(String columns) {
		detail.select(columns);
		return this;
	}

	public DefaultOrmQuery<T> join(String property) {
		return join(property, null);
	}

	public DefaultOrmQuery<T> join(String property, String columns) {
		detail.addFetchJoin(property, columns);
		return this;
	}
	
	
	public int findRowCount(){
		
		// create a copy
		DefaultOrmQuery<T> copy = copy();
		
		return server.findRowCount(copy, null);
	}

	public List<T> findList() {
		return server.findList(this, null);
	}

	public Set<T> findSet() {
		return server.findSet(this, null);
	}

	public Map<?, T> findMap() {
		return server.findMap(this, null);
	}

	public T findUnique() {
		return server.findUnique(this, null);
	}

	public DefaultOrmQuery<T> setParameter(int position, Object value) {
		return set(position, value);
	}

	public DefaultOrmQuery<T> setParameter(String paramName, Object value) {
		return set(paramName, value);
	}

	public DefaultOrmQuery<T> bind(int position, Object value) {
		return set(position, value);
	}

	public DefaultOrmQuery<T> bind(String name, Object value) {
		return set(name, value);
	}

	/**
	 * Set an ordered bind parameter according to its position. Note that the
	 * position starts at 1 to be consistent with JDBC PreparedStatement. You
	 * need to set a parameter value for each ? you have in the query.
	 */
	public DefaultOrmQuery<T> set(int position, Object value) {
		if (bindParams == null) {
			bindParams = new BindParams();
		}
		bindParams.setParameter(position, value);
		return this;
	}

	/**
	 * Set a named bind parameter. Named parameters have a colon to prefix the
	 * name.
	 */
	public DefaultOrmQuery<T> set(String name, Object value) {
		if (bindParams == null) {
			bindParams = new BindParams();
		}
		bindParams.setParameter(name, value);
		return this;
	}

	/**
	 * Return the order by clause.
	 */
	public String getOrderBy() {
		return attributes.getOrderBy();
	}

	/**
	 * Return the order by clause.
	 */
	public String getWhere() {
		return attributes.getWhere();
	}

	/**
	 * Set the order by clause.
	 */
	public DefaultOrmQuery<T> orderBy(String orderBy) {
		attributes.setOrderBy(orderBy);
		return this;
	}

	public DefaultOrmQuery<T> setOrderBy(String orderBy) {
		return orderBy(orderBy);
	}

	/**
	 * return true if this query uses DISTINCT.
	 */
	public boolean isDistinct() {
		return distinct;
	}

	/**
	 * Set whether this query uses DISTINCT.
	 */
	public DefaultOrmQuery<T> setDistinct(boolean isDistinct) {
		this.distinct = isDistinct;
		return this;
	}

	/**
	 * Return the findListener is one has been set.
	 */
	public QueryListener<T> getListener() {
		return queryListener;
	}

	/**
	 * Set a FindListener. This is designed for large fetches where lots are
	 * rows are to be processed and instead of returning all the rows they are
	 * processed one at a time.
	 * <p>
	 * Note that the returning List Set or Map will be empty.
	 * </p>
	 */
	public DefaultOrmQuery<T> setListener(QueryListener<T> queryListener) {
		this.queryListener = queryListener;
		return this;
	}

	public Class<T> getBeanType() {
		return beanType;
	}

	public void setDetail(OrmQueryDetail detail) {
		this.detail = detail;
	}

	public OrmQueryDetail getDetail() {
		return detail;
	}

	/**
	 * Return any beans that should be added to the persistence context prior to
	 * executing the query.
	 */
	public final ArrayList<EntityBean> getContextAdditions() {
		return contextAdditions;
	}

	/**
	 * Add a bean to the context additions.
	 * <p>
	 * These are added to the persistence context before executing the query.
	 * </p>
	 */
	public void contextAdd(EntityBean bean) {
		if (contextAdditions == null) {
			contextAdditions = new ArrayList<EntityBean>();
		}
		contextAdditions.add(bean);
	}

	public String toString() {
		return "Query [" + whereExpressions + "]";
	}

	public TableJoin getIncludeTableJoin() {
		return includeTableJoin;
	}

	public void setIncludeTableJoin(TableJoin includeTableJoin) {
		this.includeTableJoin = includeTableJoin;
	}

	public int getFirstRow() {
		return attributes.getFirstRow();
	}

	public DefaultOrmQuery<T> setFirstRow(int firstRow) {
		attributes.setFirstRow(firstRow);
		return this;
	}

	public int getMaxRows() {
		return attributes.getMaxRows();
	}

	public DefaultOrmQuery<T> setMaxRows(int maxRows) {
		attributes.setMaxRows(maxRows);
		return this;
	}

	public String getMapKey() {
		return mapKey;
	}

	public DefaultOrmQuery<T> setMapKey(String mapKey) {
		this.mapKey = mapKey;
		return this;
	}

	public int getBackgroundFetchAfter() {
		return backgroundFetchAfter;
	}

	public DefaultOrmQuery<T> setBackgroundFetchAfter(int backgroundFetchAfter) {
		this.backgroundFetchAfter = backgroundFetchAfter;
		return this;
	}

	public int getInitialCapacity() {
		return initialCapacity;
	}

	public DefaultOrmQuery<T> setInitialCapacity(int initialCapacity) {
		this.initialCapacity = initialCapacity;
		return this;
	}

	public Object getId() {
		return id;
	}

	public DefaultOrmQuery<T> setId(Object id) {
		this.id = id;
		return this;
	}

	public BindParams getBindParams() {
		return bindParams;
	}

	public String getQuery() {
		return query;
	}

	public DefaultOrmQuery<T> addWhere(String addToWhereClause) {
		return where(addToWhereClause);
	}

	public DefaultOrmQuery<T> addWhere(Expression expression) {
		return where(expression);
	}

	public ExpressionList<T> addWhere() {
		return where();
	}

	public DefaultOrmQuery<T> where(String addToWhereClause) {
		if (additionalWhere == null) {
			additionalWhere = addToWhereClause;
		} else {
			additionalWhere += " " + addToWhereClause;
		}
		return this;
	}

	public DefaultOrmQuery<T> where(Expression expression) {
		if (whereExpressions == null) {
			whereExpressions = new DefaultExpressionList<T>(this);
		}
		whereExpressions.add(expression);
		return this;
	}

	public ExpressionList<T> where() {
		if (whereExpressions == null) {
			whereExpressions = new DefaultExpressionList<T>(this);
		}
		return whereExpressions;
	}

	public DefaultOrmQuery<T> addHaving(String addToHavingClause) {
		return having(addToHavingClause);
	}

	public DefaultOrmQuery<T> addHaving(Expression expression) {
		return having(expression);
	}

	public ExpressionList<T> addHaving() {
		return having();
	}

	public DefaultOrmQuery<T> having(String addToHavingClause) {
		if (additionalHaving == null) {
			additionalHaving = addToHavingClause;
		} else {
			additionalHaving += " " + addToHavingClause;
		}
		return this;
	}

	public DefaultOrmQuery<T> having(Expression expression) {
		if (havingExpressions == null) {
			havingExpressions = new DefaultExpressionList<T>(this);
		}
		havingExpressions.add(expression);
		return this;
	}

	public ExpressionList<T> having() {
		if (havingExpressions == null) {
			havingExpressions = new DefaultExpressionList<T>(this);
		}
		return havingExpressions;
	}

	public SpiExpressionList<T> getHavingExpressions() {
		return havingExpressions;
	}

	public SpiExpressionList<T> getWhereExpressions() {
		return whereExpressions;
	}

	/**
	 * Return true if using background fetching or a queryListener.
	 */
	public boolean useOwnTransaction() {
		if (backgroundFetchAfter > 0 || queryListener != null) {
			return true;
		}
		return false;
	}

	public String getGeneratedSql() {
		return generatedSql;
	}

	public void setGeneratedSql(String generatedSql) {
		this.generatedSql = generatedSql;
	}

}