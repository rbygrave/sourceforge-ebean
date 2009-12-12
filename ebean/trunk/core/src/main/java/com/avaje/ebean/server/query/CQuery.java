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
package com.avaje.ebean.server.query;

import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.Query;
import com.avaje.ebean.QueryListener;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.BeanCollectionAdd;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.bean.NodeUsageListener;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.internal.LoadContext;
import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.internal.SpiTransaction;
import com.avaje.ebean.internal.SpiQuery.Mode;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.core.ReferenceOptions;
import com.avaje.ebean.server.deploy.BeanCollectionHelp;
import com.avaje.ebean.server.deploy.BeanCollectionHelpFactory;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.querydefn.OrmQueryDetail;
import com.avaje.ebean.server.querydefn.OrmQueryProperties;
import com.avaje.ebean.server.transaction.DefaultPersistenceContext;
import com.avaje.ebean.server.type.DataBind;
import com.avaje.ebean.server.type.DataReader;
import com.avaje.ebean.server.type.RsetDataReader;

/**
 * An object that represents a SqlSelect statement.
 * <p>
 * The SqlSelect is based on a tree (Object Graph). The tree is traversed to see
 * what parts are included in the tree according to the value of
 * find.getInclude();
 * </p>
 * <p>
 * The tree structure is flattened into a SqlSelectChain. The SqlSelectChain is
 * the key object used in reading the flat resultSet back into Objects.
 * </p>
 */
public class CQuery<T> implements DbReadContext, CancelableQuery {

	private static final Logger logger = Logger.getLogger(CQuery.class.getName());

	private static final int GLOBAL_ROW_LIMIT = 1000000;

	/**
	 * The resultSet rows read.
	 */
	private int rowCount;

	/**
	 * The number of master EntityBeans loaded.
	 */
	private int loadedBeanCount;

	/**
	 * Flag set when no more rows are in the resultSet.
	 */
	private boolean noMoreRows;
	/**
	 * Id of loaded 'master' bean.
	 */
	private Object loadedBeanId;
	/**
	 * Flag set when 'master' bean changed.
	 */
	boolean loadedBeanChanged;
	/**
	 * The 'master' bean just loaded.
	 */
	private Object loadedBean;

	/**
	 * Holds the previous loaded bean.
	 */
	private Object prevLoadedBean;

	/**
	 * The detail bean just loaded.
	 */
	Object loadedManyBean;

	/**
	 * The previous 'detail' collection remembered so that for manyToMany we can
	 * turn on the modify listening.
	 */
	private BeanCollection<?> prevDetailCollection;

	/**
	 * The current 'detail' collection being populated.
	 */
	private BeanCollection<?> currentDetailCollection;

	/**
	 * The 'master' collection being populated.
	 */
	private final BeanCollection<T> collection;
	/**
	 * The help for the 'master' collection.
	 */
	private final BeanCollectionHelp<T> help;

	/**
	 * The overall find request wrapper object.
	 */
	private final OrmQueryRequest<T> request;

	private final BeanDescriptor<T> desc;

	private final SpiQuery<T> query;
	
	private final OrmQueryDetail queryDetail;

	private final QueryListener<T> queryListener;

	private final boolean sharedInstance;
	
	private final boolean readOnly;
	
	private Map<String,String> currentPathMap;

	private String currentPrefix;
	
	/**
	 * Flag set true when reading 'master' and 'detail' beans.
	 */
	private final boolean manyIncluded;

	/**
	 * Where clause predicates.
	 */
	private final CQueryPredicates predicates;

	/**
	 * Object handling the SELECT generation and reading.
	 */
	private final SqlTree selectClause;

	private final boolean rawSql;

	/**
	 * The final sql that is generated.
	 */
	private final String sql;

	/**
	 * Where clause to show in logs when using an existing query plan.
	 */
	private final String logWhereSql;

	/**
	 * Set to true if the row number column is included in the sql.
	 */
	private final boolean rowNumberIncluded;

	/**
	 * Tree that knows how to build the master and detail beans from the
	 * resultSet.
	 */
	private final SqlTreeNode rootNode;

	/**
	 * For master detail query.
	 */
	private final BeanPropertyAssocMany<?> manyProperty;

	private final int backgroundFetchAfter;

	private final int maxRowsLimit;

	/**
	 * Flag set when max rows hit.
	 */
	private boolean hasHitMaxRows;

	/**
	 * Flag set when backgroundFetchAfter limit is hit.
	 */
	private boolean hasHitBackgroundFetchAfter;

	private final PersistenceContext persistenceContext;

	private RsetDataReader dataReader;

	/**
	 * The statement used to create the resultSet.
	 */
	private PreparedStatement pstmt;

	private boolean cancelled;
	
	private String bindLog;

	private final CQueryPlan queryPlan;

	/**
	 * A double check to make sure the beans are being loaded in the correct
	 * order. This is not necessary so could be removed at some point.
	 */
	private HashSet<Object> loadBeanOrderCheck;

	private long startNano;
	
	private final Mode queryMode;
	
	private final boolean autoFetchProfiling;
	
	private final ObjectGraphNode autoFetchParentNode;
	
	private final AutoFetchManager autoFetchManager;
	private final WeakReference<NodeUsageListener> autoFetchManagerRef;
	
	private final HashMap<String,ReferenceOptions> referenceOptionsMap = new HashMap<String,ReferenceOptions>();

	private int executionTimeMicros;
	
	private final int parentState;
	
	/**
	 * Create the Sql select based on the request.
	 */
	public CQuery(OrmQueryRequest<T> request, CQueryPredicates predicates, CQueryPlan queryPlan) {
		this.request = request;
		this.queryPlan = queryPlan;
		this.query = request.getQuery();
		this.queryDetail = query.getDetail();
		this.queryMode = query.getMode();
		
		this.sharedInstance = query.isSharedInstance();
		this.readOnly = request.isReadOnly();
		
		this.parentState = request.getParentState();
		
		autoFetchManager = query.getAutoFetchManager();
		autoFetchProfiling = autoFetchManager != null;
		autoFetchParentNode = autoFetchProfiling ? query.getParentNode() : null;
		
		autoFetchManagerRef = autoFetchProfiling ? new WeakReference<NodeUsageListener>(autoFetchManager) : null;
		
		// set the generated sql back to the query
		// so its available to the user...
		query.setGeneratedSql(queryPlan.getSql());

		this.selectClause = queryPlan.getSelectClause();
		this.rootNode = selectClause.getRootNode();
		this.manyProperty = selectClause.getManyProperty();
		this.manyIncluded = selectClause.isManyIncluded();
		if (manyIncluded) {
			// a error checking mechanism... that I will
			// probably remove at some point - checks order
			// of the duplicate master beans in a master/detail
			// type query...
			loadBeanOrderCheck = new HashSet<Object>(200);
		}

		this.sql = queryPlan.getSql();
		this.rawSql = queryPlan.isRawSql();
		this.rowNumberIncluded = queryPlan.isRowNumberIncluded();
		this.logWhereSql = queryPlan.getLogWhereSql();
		this.desc = request.getBeanDescriptor();
		this.predicates = predicates;

		queryListener = query.getListener();
		if (queryListener == null) {
			// normal, use the one from the transaction
			this.persistenceContext = request.getPersistenceContext();
		} else {
			// 'Row Level Transaction Context'...
			// local transaction context that will be reset
			// after each 'master' bean is sent to the listener
			this.persistenceContext = new DefaultPersistenceContext();
		}

		maxRowsLimit = query.getMaxRows() > 0 ? query.getMaxRows() : GLOBAL_ROW_LIMIT;
		backgroundFetchAfter = query.getBackgroundFetchAfter() > 0 ? query.getBackgroundFetchAfter() : Integer.MAX_VALUE;

		help = createHelp(request);
		collection = help != null ? help.createEmpty() : null;
	}

	private BeanCollectionHelp<T> createHelp(OrmQueryRequest<T> request) {
		if (request.isFindById()) {
			return null;
		} else {
			Query.Type manyType = request.getQuery().getType();
			if (manyType == null){
				// subQuery compiled for InQueryExpression
				return null;
			}
			return BeanCollectionHelpFactory.create(request);
		}
	}
		
	public DataReader getDataReader() {
        return dataReader;
    }

    public Mode getQueryMode() {
		return queryMode;
	}

	/**
	 * Return true if the query is to a lazy load for a bean in the cache.
	 */
	public boolean isSharedInstance() {
		return sharedInstance;
	}

	/**
	 * The entities should be returned in readOnly mode.
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

	public CQueryPredicates getPredicates() {
		return predicates;
	}
	
	public LoadContext getGraphContext() {
		return request.getGraphContext();
	}

	public OrmQueryRequest<?> getQueryRequest() {
		return request;
	}

	public void cancel() {
		synchronized (this) {
			this.cancelled = true;
			if (pstmt != null){
				try {
					pstmt.cancel();
				} catch (SQLException e){
					String msg = "Error cancelling query";
					throw new PersistenceException(msg, e);
				}
			}
		}
	}
	
	public boolean prepareBindExecuteQuery() throws SQLException {

		synchronized (this) {
			if (cancelled || query.isCancelled()){
				// cancelled before we started
				cancelled = true;
				return false;
			}
		
			startNano = System.nanoTime();
			
			// prepare
			SpiTransaction t = request.getTransaction();
			Connection conn = t.getInternalConnection();
			pstmt = conn.prepareStatement(sql);
	
			if (query.getTimeout() > 0){
				pstmt.setQueryTimeout(query.getTimeout());
			}
			if (query.getBufferFetchSizeHint() > 0){
				pstmt.setFetchSize(query.getBufferFetchSizeHint());
			}
			
			// bind
			DataBind dataBind = new DataBind(pstmt);
			bindLog = predicates.bind(dataBind);
	
			// executeQuery
			ResultSet rset = pstmt.executeQuery();
			dataReader = new RsetDataReader(rset);
			return true;
		}
	}

	/**
	 * Close the resources.
	 * <p>
	 * The jdbc resultSet and statement need to be closed. Its important that
	 * this method is called.
	 * </p>
	 */
	public void close() {
		try {
			if (dataReader != null) {
	            dataReader.close();
	            dataReader = null;
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, null, e);
		}
		try {
			if (pstmt != null) {
				pstmt.close();
				pstmt = null;
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, null, e);
		}
	}
	
	/**
	 * Return the reference options used to define cache use.
	 */
	public ReferenceOptions getReferenceOptionsFor(BeanPropertyAssocOne<?> beanProp) {
	
		String beanPropName = beanProp.getName();
		if (currentPrefix != null){
			beanPropName = currentPrefix+"."+beanPropName;
		}
		ReferenceOptions opt = referenceOptionsMap.get(beanPropName);
		if (opt == null){
			OrmQueryProperties chunk = queryDetail.getChunk(beanPropName, false);
			if (chunk != null) {
				// get the options from the query
				opt = chunk.getReferenceOptions();
			} 
			if (opt == null){
				// get the default options defined for the target bean type
				opt = beanProp.getTargetDescriptor().getReferenceOptions();
			}
			referenceOptionsMap.put(beanPropName, opt);
		}

		return opt;
	}

	/**
	 * Return the persistence context.
	 */
	public PersistenceContext getPersistenceContext(){
		return persistenceContext;
	}
	
	public void setLoadedBean(Object bean, Object id) {
		if (id != null && id.equals(loadedBeanId)) {
			// master/detail loading with master bean
			// unchanged. NB Using id to avoid any issue
			// with equals not being implemented

		} else {
			if (manyIncluded) {
				if (rowCount > 1) {
					loadedBeanChanged = true;
				}
				this.prevLoadedBean = loadedBean;
				this.loadedBeanId = id;
				if (!loadBeanOrderCheck.add(id)) {
					String msg = "The ordering of beans being loaded is not correct? id:" + id
							+ " already loaded at rowCount:" + rowCount;
					throw new PersistenceException(msg);
				}
			}
			this.loadedBean = bean;
		}
	}

	public void setLoadedManyBean(Object manyValue) {
		this.loadedManyBean = manyValue;
	}

	/**
	 * Return the last read bean.
	 */
	@SuppressWarnings("unchecked")
	public T getLoadedBean() {
		if (manyIncluded) {
			if (prevDetailCollection != null) {
				prevDetailCollection.setModifyListening(manyProperty.getModifyListenMode());
			} else {
				currentDetailCollection.setModifyListening(manyProperty.getModifyListenMode());
			}
		}

		if (prevLoadedBean != null) {
			return (T)prevLoadedBean;
		} else {
			return (T)loadedBean;
		}
	}

	/**
	 * Read a row from the result set returning a bean.
	 * <p>
	 * If the query includes a many then the first object in the returned array
	 * is the one/master and the second the many/detail.
	 * </p>
	 */
	private boolean readRow() throws SQLException {

		synchronized (this) {
			if (cancelled){
				return false;
			}
		
			if (!dataReader.next()){
			    return false;
			}

			rowCount++;
			dataReader.resetColumnPosition();
			
			if (rowNumberIncluded) {
				// row_number() column used for limit features
			    dataReader.incrementPos(1);
			}
	
			rootNode.load(this, null, parentState);
	
			return true;
		}
	}
	
	public int getQueryExecutionTimeMicros(){
		return executionTimeMicros;
	}
	
	public boolean readBean() throws SQLException {
		
		boolean result = readBeanInternal();

		long exeNano = System.nanoTime() - startNano;
		executionTimeMicros = (int)exeNano/1000;
		
		if (autoFetchProfiling){			
			autoFetchManager.collectQueryInfo(autoFetchParentNode, loadedBeanCount, executionTimeMicros);	
		}		
		
		queryPlan.executionTime(loadedBeanCount, executionTimeMicros);
		
		return result;
	}
	
	private boolean readBeanInternal() throws SQLException {
		if (!manyIncluded) {
			// simple query... no details...
			return readRow();
		}

		if (noMoreRows) {
			return false;
		}

		if (rowCount == 0) {
			if (!readRow()) {
				// no rows at all...
				return false;
			} else {
				createNewDetailCollection();
			}
		}

		if (readIntoCurrentDetailCollection()) {
			createNewDetailCollection();
			// return prevLoadedBean
			return true;

		} else {
			// return loadedBean
			prevDetailCollection = null;
			prevLoadedBean = null;
			noMoreRows = true;
			return true;
		}
	}

	private boolean readIntoCurrentDetailCollection() throws SQLException {
		while (readRow()) {
			if (loadedBeanChanged) {
				loadedBeanChanged = false;
				return true;
			} else {
				addToCurrentDetailCollection();
			}
		}
		return false;
	}

	private BeanCollectionAdd currentDetailAdd;
	
	private void createNewDetailCollection() {
		prevDetailCollection = currentDetailCollection;
		if (queryMode.equals(Mode.LAZYLOAD_MANY)){
			// just populate the current collection
			currentDetailCollection = (BeanCollection<?>)manyProperty.getValue(loadedBean);
		} else {
			// create a new collection to populate and assign to the bean
			currentDetailCollection = manyProperty.createEmpty();
			manyProperty.setValue(loadedBean, currentDetailCollection);
		}
		// the manyKey is always null for this case, just using default mapKey on the property
		currentDetailAdd = manyProperty.getBeanCollectionAdd(currentDetailCollection, null);
		addToCurrentDetailCollection();
	}

	private void addToCurrentDetailCollection() {
		if (loadedManyBean != null) {
			currentDetailAdd.addBean(loadedManyBean);
			//manyProperty.add(currentDetailCollection, loadedManyBean);
		}
	}

	public BeanCollection<T> continueFetchingInBackground() throws SQLException {
		readTheRows(false);
		collection.setFinishedFetch(true);
		return collection;
	}

	public BeanCollection<T> readCollection() throws SQLException {

		readTheRows(true);

		long exeNano = System.nanoTime() - startNano;
		executionTimeMicros = (int)exeNano/1000;
		
		if (autoFetchProfiling){
			autoFetchManager.collectQueryInfo(autoFetchParentNode, loadedBeanCount, executionTimeMicros);	
		}
		
		queryPlan.executionTime(loadedBeanCount, executionTimeMicros);
		
		return collection;
	}

	private void readTheRows(boolean inForeground) throws SQLException {
		while (readBeanInternal()) {
			loadedBeanCount++;
			
			if (loadedBeanCount >= maxRowsLimit) {
				hasHitMaxRows = true;
			} else if (loadedBeanCount >= backgroundFetchAfter) {
				hasHitBackgroundFetchAfter = true;
			}
			
			if (queryListener != null) {
				queryListener.process(getLoadedBean());
				// clear the transaction context after each
				// 'master' bean has been sent to the listener
				persistenceContext.clear();

			} else {
				// add to the list/set/map
				help.add(collection, getLoadedBean());
			}

			if (hasHitMaxRows) {
				boolean hasMoreRows = readRow();
				collection.setHasMoreRows(hasMoreRows);
				break;

			} else if (inForeground && hasHitBackgroundFetchAfter) {
				collection.setFinishedFetch(false);
				break;
			}
		}
	}

	public String getLoadedRowDetail() {
		if (!manyIncluded) {
			return String.valueOf(rowCount);
		} else {
			return loadedBeanCount + ":" + rowCount;
		}
	}

	public void register(String path, EntityBeanIntercept ebi){
		
		path = getPath(path);
		request.getGraphContext().register(path, ebi);
	}

	public void register(String path, BeanCollection<?> bc){
		
		path = getPath(path);
		request.getGraphContext().register(path, bc);
	}
	
	
	public boolean useBackgroundToContinueFetch() {
		return hasHitBackgroundFetchAfter;
	}

	/**
	 * Return the query name.
	 */
	public String getName() {
		return query.getName();
	}

	/**
	 * Return true if this is a raw sql query as opposed to Ebean generated sql.
	 */
	public boolean isRawSql() {
		return rawSql;
	}

	/**
	 * Return the where predicate for display in the transaction log.
	 */
	public String getLogWhereSql() {
		return logWhereSql;
	}

	/**
	 * Return the property that is associated with the many. There can only be
	 * one per SqlSelect. This can be null.
	 */
	public BeanPropertyAssocMany<?> getManyProperty() {
		return manyProperty;
	}

	/**
	 * Get the summary of the sql.
	 */
	public String getSummary() {
		return selectClause.getSummary();
	}

	/**
	 * Return the SqlSelectChain. This is the flattened structure that
	 * represents this query.
	 */
	public SqlTree getSelectClause() {
		return selectClause;
	}

	public String getBindLog() {
		return bindLog;
	}

	public SpiTransaction getTransaction() {
		return request.getTransaction();
	}

	public String getBeanType() {
		return desc.getFullName();
	}

	/**
	 * Return the short bean name.
	 */
	public String getBeanName() {
        return desc.getName();
    }
	
	/**
	 * Return the generated sql.
	 */
	public String getGeneratedSql() {
		return sql;
	}
	
	/**
	 * Should we create profileNodes for beans created in this query.
	 * <p>
	 * This is true for all queries except lazy load bean queries.
	 * </p>
	 */
	public boolean isAutoFetchProfiling() {
		// need query.isProfiling() because we just take the data
		// from the lazy loaded or refreshed beans and put it into the already 
		// existing beans which are already collecting usage information
		return autoFetchProfiling && query.isUsageProfiling();
	}

	private String getPath(String propertyName) {
		
		if (currentPrefix == null){
			return propertyName;
		} else if (propertyName == null) {
			return currentPrefix;
		}
		
		String path = currentPathMap.get(propertyName);
		if (path != null){
			return path;
		} else {
			return currentPrefix+"."+propertyName;
		}
	}
	
	
	public void profileBean(EntityBeanIntercept ebi, String prefix) {
		
		ObjectGraphNode node = request.getGraphContext().getObjectGraphNode(prefix);
		
		ebi.setNodeUsageCollector(new NodeUsageCollector(node, autoFetchManagerRef));
	}

	public void setCurrentPrefix(String currentPrefix, Map<String,String> currentPathMap) {
		this.currentPrefix = currentPrefix;
		this.currentPathMap = currentPathMap;
	}
	
}
