package com.avaje.ebeaninternal.server.querydefn;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.Expression;
import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.FetchConfig;
import com.avaje.ebean.FutureIds;
import com.avaje.ebean.FutureList;
import com.avaje.ebean.FutureRowCount;
import com.avaje.ebean.JoinConfig;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.OrderBy.Property;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;
import com.avaje.ebean.QueryIterator;
import com.avaje.ebean.QueryListener;
import com.avaje.ebean.QueryResultVisitor;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.bean.BeanCollectionTouched;
import com.avaje.ebean.bean.CallStack;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.ObjectGraphOrigin;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.meta.MetaAutoFetchStatistic;
import com.avaje.ebeaninternal.api.BindParams;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionList;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.autofetch.AutoFetchManager;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.DRawSqlSelect;
import com.avaje.ebeaninternal.server.deploy.DeployNamedQuery;
import com.avaje.ebeaninternal.server.deploy.TableJoin;
import com.avaje.ebeaninternal.server.expression.SimpleExpression;
import com.avaje.ebeaninternal.server.query.CancelableQuery;
import com.avaje.ebeaninternal.util.DefaultExpressionList;

import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of an Object Relational query.
 */
public class DefaultOrmQuery<T> implements SpiQuery<T> {

	private static final long serialVersionUID = 6838006264714672460L;

	private final Class<T> beanType;
	
	private transient final EbeanServer server;
	
	private transient BeanCollectionTouched beanCollectionTouched; 
	
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

	private transient BeanDescriptor<?> beanDescriptor;
	
	private boolean cancelled;
	
	private transient CancelableQuery cancelableQuery;
	
	/**
	 * The name of the query.
	 */
	private String name;
	
	private UseIndex useIndex;
	
	private Type type;
	
	private Mode mode = Mode.NORMAL;
	
	/**
	 * Holds query in structured form.
	 */
	private OrmQueryDetail detail;

	private int maxRows;

	private int firstRow;

	private int totalHits;
	
	/**
	 * The where clause from a parsed query string.
	 */
	private String rawWhereClause;
	
	private OrderBy<T> orderBy;

	private String loadMode;
	private String loadDescription;
	
	private String generatedSql;

	/**
	 * Query language version of the query.
	 */
	private String query;

	private String additionalWhere;

	private String additionalHaving;

	private String lazyLoadProperty;
	
    private String lazyLoadManyPath;

	/**
	 * Set to true when we want to return vanilla (not enhanced) objects.
	 */
	private Boolean vanillaMode;
	
	/**
	 * Set to true if you want a DISTINCT query.
	 */
	private boolean distinct;
	
	/**
	 * Set to true if this is a future fetch using background threads.
	 */
	private boolean futureFetch;
	
//	/**
//	 * Set to true when this is a lazy load for a sharedInstance.
//	 */
//	private boolean sharedInstance;
	
	private List<Object> partialIds;
	
	/**
	 * The rows after which the fetch continues in a bg thread.
	 */
	private int backgroundFetchAfter;

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

	private int bufferFetchSizeHint;
	
	private boolean usageProfiling = true;

	private boolean loadBeanCache;
	private Boolean useBeanCache;
	
	private Boolean useQueryCache;

	private Boolean readOnly;
	
	private boolean sqlSelect;

	/**
	 * Allow for explicit on off or null for default.
	 */
	private Boolean autoFetch;

    /**
     * Allow to fetc a record "for update" which should lock it on read
     */
    private Boolean forUpdate;
	
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

	/**
	 * Hash of final query after AutoFetch tuning.
	 */
	private int queryPlanHash;
	
	private transient PersistenceContext persistenceContext;

  private ManyWhereJoins manyWhereJoins;
    
  private RawSql rawSql;
	
	public DefaultOrmQuery(Class<T> beanType, EbeanServer server, ExpressionFactory expressionFactory, String query) {
		this.beanType = beanType;
		this.server = server;
		this.expressionFactory = expressionFactory;
		this.detail = new OrmQueryDetail();
		this.name = "";
		if (query != null){
		    setQuery(query);
		}
	}

	/**
	 * Additional supply a query which is parsed.
	 */
	public DefaultOrmQuery(Class<T> beanType, EbeanServer server, ExpressionFactory expressionFactory, 
	        DeployNamedQuery namedQuery) throws PersistenceException {

		this.beanType = beanType;
		this.server = server;
		this.expressionFactory = expressionFactory;
        this.detail = new OrmQueryDetail();
        if (namedQuery == null){
            this.name = "";
        } else {
    		this.name = namedQuery.getName();
    		this.sqlSelect = namedQuery.isSqlSelect();
    		if (sqlSelect) {
    			// potentially with where and having clause...
    			DRawSqlSelect sqlSelect = namedQuery.getSqlSelect();
    			additionalWhere = sqlSelect.getWhereClause();
    			additionalHaving = sqlSelect.getHavingClause();
    		} else if (namedQuery.isRawSql()){
    		    rawSql = namedQuery.getRawSql();
    		    
    		} else {
    			// parse the entire query...
    			setQuery(namedQuery.getQuery());
    		}
        }
	}
	
	public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

    /**
	 * Set the BeanDescriptor for the root type of this query.
	 */
	public void setBeanDescriptor(BeanDescriptor<?> beanDescriptor) {
		this.beanDescriptor = beanDescriptor;
	}

	/**
	 * Return true if select all properties was used to ensure the
	 * property invoking a lazy load was included in the query.
	 */
	public boolean selectAllForLazyLoadProperty() {
		if (lazyLoadProperty != null){
			if (!detail.containsProperty(lazyLoadProperty)){
				detail.select("*");
				return true;
			}
		}
		return false;
	}
	
	public RawSql getRawSql() {
        return rawSql;
    }

    public DefaultOrmQuery<T> setRawSql(RawSql rawSql) {
        this.rawSql = rawSql;
        return this;
    }

    public String getLazyLoadProperty() {
        return lazyLoadProperty;
    }

    public void setLazyLoadProperty(String lazyLoadProperty) {
		this.lazyLoadProperty = lazyLoadProperty;
	}

	public String getLazyLoadManyPath() {
        return lazyLoadManyPath;
    }

    public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public MetaAutoFetchStatistic getMetaAutoFetchStatistic() {
	    if (parentNode != null && server != null){
	        ObjectGraphOrigin origin = parentNode.getOriginQueryPoint();
	        return server.find(MetaAutoFetchStatistic.class, origin.getKey());
	    }
	    return null;
	}
	
    /**
     * Return true if the where expressions contains a many property.
     */
    public boolean initManyWhereJoins() {
        manyWhereJoins = new ManyWhereJoins();
        if (whereExpressions != null){
            whereExpressions.containsMany(beanDescriptor, manyWhereJoins);
        }
        return !manyWhereJoins.isEmpty();
    }
    
    public ManyWhereJoins getManyWhereJoins() {
        return manyWhereJoins;
    }

    public List<OrmQueryProperties> removeQueryJoins() {
        List<OrmQueryProperties> queryJoins =  detail.removeSecondaryQueries();
        if (queryJoins != null){
            if (orderBy != null){
                // remove any orderBy properties that relate to 
                // paths of the secondary queries
                for (int i = 0; i < queryJoins.size(); i++) {
                    OrmQueryProperties joinPath = queryJoins.get(i);
                    
                    // loop through the orderBy properties and
                    // move any ones related to the query join
                    List<Property> properties = orderBy.getProperties();
                    Iterator<Property> it = properties.iterator();
                    while (it.hasNext()) {
                        OrderBy.Property property = it.next();
                        if (property.getProperty().startsWith(joinPath.getPath())){
                            // remove this orderBy segment and 
                            // add it to the secondary join
                            it.remove();
                            joinPath.addSecJoinOrderProperty(property);
                        }
                    }
                }
            }
        }
        return queryJoins;
    }
    
    public List<OrmQueryProperties> removeLazyJoins() {
        return detail.removeSecondaryLazyQueries();
    }
        
    public void setLazyLoadManyPath(String lazyLoadManyPath) {
        this.lazyLoadManyPath = lazyLoadManyPath;
    }

    /**
     * Convert any many joins fetch joins to query joins.
     */
    public void convertManyFetchJoinsToQueryJoins(boolean allowOne, int queryBatch) {
        detail.convertManyFetchJoinsToQueryJoins(beanDescriptor, lazyLoadManyPath, allowOne, queryBatch);
    }
	
	/**
	 * Set the select clause to select the Id property.
	 */
	public void setSelectId() {
		// clear select and fetch joins..
		detail.clear();
		
		select(beanDescriptor.getIdBinder().getIdProperty());
	}
	
	public void convertWhereNaturalKeyToId(Object idValue) {
		whereExpressions = new DefaultExpressionList<T>(this, null);
		setId(idValue);
	}
	
	public NaturalKeyBindParam getNaturalKeyBindParam() {
		NaturalKeyBindParam namedBind = null;
		if (bindParams != null){
			namedBind = bindParams.getNaturalKeyBindParam();
			if (namedBind == null) {
				return null;
			}
		}
		
		if (whereExpressions != null){
			List<SpiExpression> exprList = whereExpressions.internalList();
			if (exprList.size() > 1){
				return null;
			} else if (exprList.size() == 0) {
				return namedBind;
			} else {
				if (namedBind != null){
					return null;
				}
				SpiExpression se = exprList.get(0);
				if (se instanceof SimpleExpression){
					SimpleExpression e = (SimpleExpression)se;
					if (e.isOpEquals()){
						return new NaturalKeyBindParam(e.getPropertyName(), e.getValue()); 
					}
				}
			}
		}
		return null;
	}
	
	public DefaultOrmQuery<T> copy() {
		// Not including these in the copy:
		// contextAdditions
		// queryListener
		// transactionContext
        // autoFetchTuned
        // autoFetchQueryPlanHash
	    // copy.generatedSql
	    
		DefaultOrmQuery<T> copy = new DefaultOrmQuery<T>(beanType, server, expressionFactory, (String)null);
		copy.name = name;
		copy.includeTableJoin = includeTableJoin;
		copy.autoFetchManager = autoFetchManager;

		copy.query = query;
		copy.additionalWhere = additionalWhere;
		copy.additionalHaving = additionalHaving;
		copy.distinct = distinct;
		copy.backgroundFetchAfter = backgroundFetchAfter;
		copy.timeout = timeout;
		copy.mapKey = mapKey;
		copy.id = id;
		copy.vanillaMode = vanillaMode;
		copy.loadBeanCache = loadBeanCache;
		copy.useBeanCache = useBeanCache;
		copy.useQueryCache = useQueryCache;
		copy.readOnly = readOnly;
		copy.sqlSelect = sqlSelect;
		if (detail != null){
			copy.detail = detail.copy();
		}

		copy.firstRow = firstRow;
		copy.maxRows = maxRows;
		copy.rawWhereClause = rawWhereClause;
		if (orderBy != null){
			copy.orderBy = orderBy.copy();			
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
        
        copy.forUpdate = forUpdate;

		return copy;
	}

	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}

	public UseIndex getUseIndex() {
        return useIndex;
    }

    public DefaultOrmQuery<T> setUseIndex(UseIndex useIndex) {
        this.useIndex = useIndex;
        return this;
    }

    public String getLoadDescription() {
		return loadDescription;
	}

	public String getLoadMode() {
		return loadMode;
	}

	public void setLoadDescription(String loadMode, String loadDescription) {
		this.loadMode = loadMode;
		this.loadDescription = loadDescription;
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
    
    public Boolean isForUpdate() {
        return forUpdate;
    }

	public DefaultOrmQuery<T> setAutoFetch(boolean autoFetch) {
		return setAutofetch(autoFetch);
	}
	
	public DefaultOrmQuery<T> setAutofetch(boolean autoFetch) {
		this.autoFetch = autoFetch;
		return this;
	}
	
    public DefaultOrmQuery<T> setForUpdate(boolean forUpdate) {
        this.forUpdate = forUpdate;
        return this;
    }

	public AutoFetchManager getAutoFetchManager() {
		return autoFetchManager;
	}

	public void setAutoFetchManager(AutoFetchManager autoFetchManager) {
		this.autoFetchManager = autoFetchManager;
	}
	
//	/**
//	 * Check other combinations that can make this a sharedInstance query. 
//	 */
//	public void deriveSharedInstance() {
//		if (!sharedInstance){
//			if (Boolean.TRUE.equals(useQueryCache)
//				|| (Boolean.TRUE.equals(readOnly) && 
//						(Boolean.TRUE.equals(useBeanCache) || Boolean.TRUE.equals(loadBeanCache)))) {
//				// these combinations also producing shared instance beans 
//				sharedInstance = true;
//			}
//		}
//	}
//	
//	public boolean isSharedInstance() {
//		return sharedInstance;
//	}
//
//	public void setSharedInstance() {
//		this.sharedInstance = true;
//	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public boolean isUsageProfiling() {
		return usageProfiling;
	}

	public void setUsageProfiling(boolean usageProfiling) {
		this.usageProfiling = usageProfiling;
	}

	public void setParentNode(ObjectGraphNode parentNode) {
		this.parentNode = parentNode;
	}

	public ObjectGraphNode getParentNode() {
		return parentNode;
	}

	
	public ObjectGraphNode setOrigin(CallStack callStack) {

		// create a 'origin' which links this query to the profiling information 
		ObjectGraphOrigin o =  new ObjectGraphOrigin(calculateOriginQueryHash(), callStack, beanType.getName());
		parentNode = new ObjectGraphNode(o, null);
		return parentNode;
	}

	/**
	 * Calculate a hash for use in determining the ObjectGraphOrigin.
	 * <p>
	 * This should be quite a stable hash as most uniqueness is determined
	 * by the CallStack, so we only use the bean type and overall query type.
	 * </p>
	 * <p>
	 * This stable hash allows the query to be changed (joins added etc) without
	 * losing the already collected usage profiling.
	 * </p>
	 */
    private int calculateOriginQueryHash() {
        int hc = beanType.getName().hashCode();
        hc = hc * 31 + (type == null ? 0 : type.ordinal());
        return hc;
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

        hc = hc * 31 + (type == null ? 0 : type.ordinal());
        hc = hc * 31 + (useIndex == null ? 0 : useIndex.hashCode());

        hc = hc * 31 + (rawSql == null ? 0 : rawSql.queryHash());

        hc = hc * 31 + (autoFetchTuned ? 31 : 0);
		hc = hc * 31 + (distinct ? 31 : 0);
        hc = hc * 31 + (query == null ? 0 : query.hashCode());
        hc = hc * 31 + detail.queryPlanHash(request);

		hc = hc * 31 + (firstRow == 0 ? 0 : firstRow);
		hc = hc * 31 + (maxRows == 0 ? 0 : maxRows);
		hc = hc * 31 + (orderBy == null ? 0 : orderBy.hash());
		hc = hc * 31 + (rawWhereClause == null ? 0 : rawWhereClause.hashCode());

		hc = hc * 31 + (additionalWhere == null ? 0 : additionalWhere.hashCode());
		hc = hc * 31 + (additionalHaving == null ? 0 : additionalHaving.hashCode());
		hc = hc * 31 + (mapKey == null ? 0 : mapKey.hashCode());
		hc = hc * 31 + (id == null ? 0 : 1);

		if (bindParams != null) {
		    hc = hc * 31 + bindParams.getQueryPlanHash();
		}
		
		if (request == null){
			// for AutoFetch...
			hc = hc * 31 + (whereExpressions == null ? 0 : whereExpressions.queryAutoFetchHash());
			hc = hc * 31 + (havingExpressions == null ? 0 : havingExpressions.queryAutoFetchHash());
			
		} else {
			// for query plan...
			hc = hc * 31 + (whereExpressions == null ? 0 : whereExpressions.queryPlanHash(request));
			hc = hc * 31 + (havingExpressions == null ? 0 : havingExpressions.queryPlanHash(request));
		}
		
        hc = hc * 31 + (forUpdate == null ? 0 : forUpdate.hashCode());

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

    public boolean isRawSql() {
        return rawSql != null;
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
		return maxRows > 0 || firstRow > 0;
	}

	
	public boolean isVanillaMode(boolean defaultVanillaMode) {
        if (vanillaMode != null) {
            return vanillaMode.booleanValue();
        }
        return defaultVanillaMode;
    }

    public DefaultOrmQuery<T> setVanillaMode(boolean vanillaMode) {
        this.vanillaMode = vanillaMode;
        return this;
    }

    public Boolean isReadOnly() {
		return readOnly;
	}

	public DefaultOrmQuery<T> setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	public Boolean isUseBeanCache() {
		return useBeanCache;
	}

	public boolean isUseQueryCache() {
		return Boolean.TRUE.equals(useQueryCache);
	}

	public DefaultOrmQuery<T> setUseCache(boolean useBeanCache) {
		this.useBeanCache = useBeanCache;
		return this;
	}
	
	public DefaultOrmQuery<T> setUseQueryCache(boolean useQueryCache) {
		this.useQueryCache = useQueryCache;
		return this;
	}

	public boolean isLoadBeanCache() {
		return loadBeanCache;
	}

	public DefaultOrmQuery<T> setLoadBeanCache(boolean loadBeanCache) {
		this.loadBeanCache = loadBeanCache;
		return this;
	}
	
	public DefaultOrmQuery<T> setTimeout(int secs) {
		this.timeout = secs;
		return this;
	}

	public DefaultOrmQuery<T> setQuery(String queryString) throws PersistenceException {

		this.query = queryString;

		OrmQueryDetailParser parser = new OrmQueryDetailParser(queryString);
		parser.parse();
		parser.assign(this);
	
		return this;
	}
	
	protected void setOrmQueryDetail(OrmQueryDetail detail){
		this.detail = detail;
	}
	protected void setRawWhereClause(String rawWhereClause){
		this.rawWhereClause = rawWhereClause;
	}

	public DefaultOrmQuery<T> setProperties(String columns) {
		return select(columns);
	}
	
	public void setDefaultSelectClause(){
		detail.setDefaultSelectClause(beanDescriptor);
	}

	public DefaultOrmQuery<T> select(String columns) {
		detail.select(columns);
		return this;
	}

	public DefaultOrmQuery<T> join(String property) {
		return join(property, null, null);
	}
	
	public DefaultOrmQuery<T> join(String property, JoinConfig joinConfig) {
		return join(property, null, joinConfig);
	}

	public DefaultOrmQuery<T> join(String property, String columns) {
		return join(property, columns, null);
	}
	
	public DefaultOrmQuery<T> join(String property, String columns, JoinConfig joinConfig) {

        FetchConfig c;
        if (joinConfig == null){
            c = null;
        } else {
            c = new FetchConfig();
            c.lazy(joinConfig.getLazyBatchSize());
            if (joinConfig.isQueryAll()){
                c.query(joinConfig.getQueryBatchSize());                
            } else {
                c.queryFirst(joinConfig.getQueryBatchSize());
            }
        }
        
	    detail.addFetch(property, columns, c);
		return this;
	}

    public DefaultOrmQuery<T> fetch(String property) {
        return fetch(property, null, null);
    }

    public DefaultOrmQuery<T> fetch(String property, FetchConfig joinConfig) {
        return fetch(property, null, joinConfig);
    }

    public DefaultOrmQuery<T> fetch(String property, String columns) {
        return fetch(property, columns, null);
    }
	
    public DefaultOrmQuery<T> fetch(String property, String columns, FetchConfig config) {
        detail.addFetch(property, columns, config);
        return this;
    }
    
	public List<Object> findIds() {
		// a copy of this query is made in the server
		// as the query needs to modified (so we modify
		// the copy rather than this query instance)
		return server.findIds(this, null);
	}
	
	public int findRowCount(){
		// a copy of this query is made in the server
		// as the query needs to modified (so we modify
		// the copy rather than this query instance)
		return server.findRowCount(this, null);
	}
	
	public void findVisit(QueryResultVisitor<T> visitor) {
        server.findVisit(this, visitor, null);
    }

    public QueryIterator<T> findIterate() {
        return server.findIterate(this, null);
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
	
	@SuppressWarnings("unchecked")
    public <K> Map<K, T> findMap(String keyProperty, Class<K> keyType) {
        setMapKey(keyProperty);
        return (Map<K, T>)findMap();
    }

    public T findUnique() {
		return server.findUnique(this, null);
	}
	
	public FutureIds<T> findFutureIds() {
		return server.findFutureIds(this, null);
	}

	public FutureList<T> findFutureList() {
		return server.findFutureList(this, null);
	}

	public FutureRowCount<T> findFutureRowCount() {
		return server.findFutureRowCount(this, null);
	}
	
	public PagingList<T> findPagingList(int pageSize) {
		return server.findPagingList(this, null, pageSize);
	}

	/**
	 * Set an ordered bind parameter according to its position. Note that the
	 * position starts at 1 to be consistent with JDBC PreparedStatement. You
	 * need to set a parameter value for each ? you have in the query.
	 */
	public DefaultOrmQuery<T> setParameter(int position, Object value) {
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
	public DefaultOrmQuery<T> setParameter(String name, Object value) {
		if (bindParams == null) {
			bindParams = new BindParams();
		}
		bindParams.setParameter(name, value);
		return this;
	}
	
	public OrderBy<T> getOrderBy() {
	    return orderBy;
	}

	/**
	 * Return the order by clause.
	 */
	public String getRawWhereClause() {
		return rawWhereClause;
	}

	public OrderBy<T> orderBy() {
		return order();
	}
	
	public OrderBy<T> order() {
		if (orderBy == null){
			orderBy = new OrderBy<T>(this, null);
		}
		return orderBy;
	}

	public DefaultOrmQuery<T> setOrderBy(String orderByClause) {
		return order(orderByClause);
	}
	
	public DefaultOrmQuery<T> orderBy(String orderByClause) {
		return order(orderByClause);
	}

	public DefaultOrmQuery<T> order(String orderByClause) {
		if (orderByClause == null || orderByClause.trim().length() == 0){
			this.orderBy = null;
		} else {
			this.orderBy = new OrderBy<T>(this, orderByClause);
		}
		return this;
	}

	public DefaultOrmQuery<T> setOrderBy(OrderBy<T> orderBy) {
		return setOrder(orderBy);
	}
	
	public DefaultOrmQuery<T> setOrder(OrderBy<T> orderBy) {
		this.orderBy = orderBy;
		if (orderBy != null){
			orderBy.setQuery(this);
		}
		return this;
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
	
	public boolean tuneFetchProperties(OrmQueryDetail tunedDetail) {
        return detail.tuneFetchProperties(tunedDetail);
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
		return firstRow;
	}

	public DefaultOrmQuery<T> setFirstRow(int firstRow) {
		this.firstRow = firstRow;
		return this;
	}

	public int getMaxRows() {
		return maxRows;
	}

	public DefaultOrmQuery<T> setMaxRows(int maxRows) {
		this.maxRows = maxRows;
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
			whereExpressions = new DefaultExpressionList<T>(this, null);
		}
		whereExpressions.add(expression);
		return this;
	}

	public ExpressionList<T> where() {
		if (whereExpressions == null) {
			whereExpressions = new DefaultExpressionList<T>(this, null);
		}
		return whereExpressions;
	}
	
	public ExpressionList<T> filterMany(String prop){
	    
	    OrmQueryProperties chunk = detail.getChunk(prop, true);
	    return chunk.filterMany(this);
	}
	

	public void setFilterMany(String prop, ExpressionList<?> filterMany){
	    if (filterMany != null){
    	    OrmQueryProperties chunk = detail.getChunk(prop, true);
    	    chunk.setFilterMany((SpiExpressionList<?>)filterMany);
	    }
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
			havingExpressions = new DefaultExpressionList<T>(this, null);
		}
		havingExpressions.add(expression);
		return this;
	}

	public ExpressionList<T> having() {
		if (havingExpressions == null) {
			havingExpressions = new DefaultExpressionList<T>(this, null);
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
	public boolean createOwnTransaction() {
		if (futureFetch){
			// the future fetches have already created
			// their own transaction
			return false;
		}
		if (backgroundFetchAfter > 0 || queryListener != null) {
			// run in own transaction as we can't know how long
			// the background fetching will continue etc
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

	public Query<T> setBufferFetchSizeHint(int bufferFetchSizeHint){
		this.bufferFetchSizeHint = bufferFetchSizeHint;
		return this;
	}
	
	public int getBufferFetchSizeHint() {
		return bufferFetchSizeHint;
	}

	public void setBeanCollectionTouched(BeanCollectionTouched notify) {
		this.beanCollectionTouched = notify;
	}

	public BeanCollectionTouched getBeanCollectionTouched() {
		return beanCollectionTouched;
	}

	public List<Object> getIdList() {
		return partialIds;
	}

	public void setIdList(List<Object> partialIds) {
		this.partialIds = partialIds;
	}

	public boolean isFutureFetch() {
		return futureFetch;
	}

	public void setFutureFetch(boolean backgroundFetch) {
		this.futureFetch = backgroundFetch;
	}

	public void setCancelableQuery(CancelableQuery cancelableQuery) {
		synchronized (this) {
			this.cancelableQuery = cancelableQuery;
		}
	}
	
	public void cancel() {
		synchronized (this) {
			cancelled = true;
			if (cancelableQuery != null){
				cancelableQuery.cancel();
			}
		}
	}

	public boolean isCancelled() {
		synchronized (this) {
			return cancelled;
		}
	}
	

}
