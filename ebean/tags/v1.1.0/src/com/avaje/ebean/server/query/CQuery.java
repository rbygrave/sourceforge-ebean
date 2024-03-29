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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.QueryListener;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.ObjectGraphOrigin;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.core.TransactionContext;
import com.avaje.ebean.server.core.TransactionContextClass;
import com.avaje.ebean.server.deploy.BeanCollectionHelp;
import com.avaje.ebean.server.deploy.BeanCollectionHelpFactory;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.ManyType;
import com.avaje.ebean.server.deploy.jointree.JoinNode;
import com.avaje.ebean.server.transaction.TransContext;

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
public class CQuery implements DbReadContext {

	private static final Logger logger = Logger.getLogger(CQuery.class.getName());

	private static final int GLOBAL_ROW_LIMIT = 1000000;

	int rsetIndex;

	/**
	 * The resultSet rows read.
	 */
	int rowCount;

	/**
	 * The number of master EntityBeans loaded.
	 */
	int loadedBeanCount;

	/**
	 * Flag set when no more rows are in the resultSet.
	 */
	boolean noMoreRows;
	/**
	 * Id of loaded 'master' bean.
	 */
	Object loadedBeanId;
	/**
	 * Flag set when 'master' bean changed.
	 */
	boolean loadedBeanChanged;
	/**
	 * The 'master' bean just loaded.
	 */
	EntityBean loadedBean;

	/**
	 * Holds the previous loaded bean.
	 */
	EntityBean prevLoadedBean;

	/**
	 * The detail bean just loaded.
	 */
	EntityBean loadedManyBean;

	/**
	 * The previous 'detail' collection remembered so that for manyToMany we can
	 * turn on the modify listening.
	 */
	BeanCollection<?> prevDetailCollection;

	/**
	 * The current 'detail' collection being populated.
	 */
	BeanCollection<?> currentDetailCollection;

	/**
	 * The 'master' collection being populated.
	 */
	final BeanCollection<?> collection;
	/**
	 * The help for the 'master' collection.
	 */
	final BeanCollectionHelp help;

	/**
	 * The overall find request wrapper object.
	 */
	final QueryRequest request;

	final BeanDescriptor desc;

	final OrmQuery<?> query;

	@SuppressWarnings("unchecked")
	final QueryListener queryListener;

	JoinNode currentJoinNode;
	
	/**
	 * When building a BeanMap result.
	 */
	final String mapKey;

	/**
	 * Flag set true when reading 'master' and 'detail' beans.
	 */
	final boolean manyIncluded;

	/**
	 * Where clause predicates.
	 */
	final CQueryPredicates predicates;

	/**
	 * Object handling the SELECT generation and reading.
	 */
	final SqlTree selectClause;

	final boolean rawSql;

	/**
	 * The final sql that is generated.
	 */
	final String sql;

	/**
	 * Where clause to show in logs when using an existing query plan.
	 */
	final String logWhereSql;

	/**
	 * Set to true if the row number column is included in the sql.
	 */
	final boolean rowNumberIncluded;

	/**
	 * Tree that knows how to build the master and detail beans from the
	 * resultSet.
	 */
	final SqlTreeNode rootNode;

	/**
	 * For master detail query.
	 */
	final BeanPropertyAssocMany manyProperty;

	final int backgroundFetchAfter;

	final int maxRowsLimit;

	/**
	 * Flag set when max rows hit.
	 */
	boolean hasHitMaxRows;

	/**
	 * Flag set when backgroundFetchAfter limit is hit.
	 */
	boolean hasHitBackgroundFetchAfter;

	final TransactionContext transactionContext;

	/**
	 * The resultSet that is read and converted to objects.
	 */
	ResultSet rset;

	/**
	 * The statement used to create the resultSet.
	 */
	PreparedStatement pstmt;

	String bindLog;

	final CQueryPlan queryPlan;

	/**
	 * A double check to make sure the beans are being loaded in the correct
	 * order. This is not necessary so could be removed at some point.
	 */
	HashSet<Object> loadBeanOrderCheck;

	long startNano;
	
	final boolean autoFetchProfiling;
	
	final ObjectGraphNode autoFetchParentNode;
	
	final AutoFetchManager autoFetchManager;
	
	final ObjectGraphOrigin autoFetchOriginQueryPoint;
	
	/**
	 * Create the Sql select based on the request.
	 */
	public CQuery(QueryRequest request, CQueryPredicates predicates, CQueryPlan queryPlan) {
		this.request = request;
		this.queryPlan = queryPlan;
		this.query = request.getQuery();
		autoFetchManager = query.getAutoFetchManager();
		autoFetchProfiling = autoFetchManager != null;
		autoFetchParentNode = autoFetchProfiling ? query.getParentNode() : null;
		autoFetchOriginQueryPoint = autoFetchProfiling ? query.getObjectGraphOrigin() : null;

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

		mapKey = query.getMapKey();
		queryListener = query.getListener();
		if (queryListener == null) {
			// normal, use the one from the transaction
			this.transactionContext = request.getTransactionContext();
		} else {
			// 'Row Level Transaction Context'...
			// local transaction context that will be reset
			// after each 'master' bean is sent to the listener
			this.transactionContext = new TransContext();
		}

		maxRowsLimit = query.getMaxRows() > 0 ? query.getMaxRows() : GLOBAL_ROW_LIMIT;
		backgroundFetchAfter = query.getBackgroundFetchAfter() > 0 ? query
				.getBackgroundFetchAfter() : Integer.MAX_VALUE;

		help = initHelp(request);
		collection = help != null ? help.createEmpty() : null;
	}

	private BeanCollectionHelp initHelp(QueryRequest request) {
		if (request.isFindById()) {
			return null;
		} else {
			ManyType manyType = request.getManyType();
			if (manyType == null){
				// subQuery compiled for InQueryExpression
				return null;
			}
			return BeanCollectionHelpFactory.create(manyType, request.getBeanDescriptor());
		}
	}
	
	public CQueryPredicates getPredicates() {
		return predicates;
	}

	public QueryRequest getQueryRequest() {
		return request;
	}

	public void prepareBindExecuteQuery() throws SQLException {

		startNano = System.nanoTime();
		
		// prepare
		ServerTransaction t = request.getTransaction();
		Connection conn = t.getInternalConnection();
		pstmt = conn.prepareStatement(sql);

		if (query.getTimeout() > 0){
			pstmt.setQueryTimeout(query.getTimeout());
		}
		
		// bind
		bindLog = predicates.bind(pstmt);

		// executeQuery
		rset = pstmt.executeQuery();
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
			if (rset != null) {
				rset.close();
				rset = null;
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

	public TransactionContextClass getClassContext(Class<?> beanType) {
		return transactionContext.getClassContext(beanType);
	}

	public void setLoadedBean(EntityBean bean, Object id) {
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

	public void setLoadedManyBean(EntityBean manyValue) {
		this.loadedManyBean = manyValue;
	}

	/**
	 * Return the last read bean.
	 */
	public EntityBean getLoadedBean() {
		if (manyIncluded && manyProperty.isManyToMany()) {
			if (prevDetailCollection != null) {
				prevDetailCollection.setModifyListening(true);
			} else {
				currentDetailCollection.setModifyListening(true);
			}
		}

		if (prevLoadedBean != null) {
			return prevLoadedBean;
		} else {
			return loadedBean;
		}
	}

	public ResultSet getRset() {
		return rset;
	}

	public int nextRsetIndex() {
		return ++rsetIndex;
	}

	/**
	 * Read a row from the result set returning a bean.
	 * <p>
	 * If the query includes a many then the first object in the returned array
	 * is the one/master and the second the many/detail.
	 * </p>
	 */
	private boolean readRow() throws SQLException {

		if (!rset.next()) {
			return false;
		}
		rowCount++;

//		if (rowCount >= maxRowsLimit) {
//			hasHitMaxRows = true;
//		} else if (rowCount >= backgroundFetchAfter) {
//			hasHitBackgroundFetchAfter = true;
//		}
		rsetIndex = 0;

		if (rowNumberIncluded) {
			// row_number() column used for limit features
			rset.getInt(++rsetIndex);
		}

		rootNode.load(this, null);

		return true;
	}

	int executionTimeMicros;
	
	public int getQueryExecutionTimeMicros(){
		return executionTimeMicros;
	}
	
	public boolean readBean() throws SQLException {
		
		boolean result = readBeanInternal();

		long exeNano = System.nanoTime() - startNano;
		executionTimeMicros = (int)exeNano/1000;
		
		if (autoFetchProfiling){
			
			autoFetchManager.collectQueryInfo(autoFetchParentNode, autoFetchOriginQueryPoint, loadedBeanCount, executionTimeMicros);	
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

	private void createNewDetailCollection() {
		prevDetailCollection = currentDetailCollection;
		currentDetailCollection = manyProperty.createEmpty();
		manyProperty.setValue(loadedBean, currentDetailCollection);
		addToCurrentDetailCollection();
	}

	private void addToCurrentDetailCollection() {
		if (loadedManyBean != null) {
			manyProperty.add(currentDetailCollection, loadedManyBean, mapKey);
		}
	}

	public void continueFetchingInBackground() throws SQLException {
		readTheRows(false);
		collection.setFinishedFetch(true);
	}

	public BeanCollection<?> readCollection(boolean useResultSetLimit) throws SQLException {
		
		if (useResultSetLimit) {
			if (!navigateFirst()) {
				return collection;
			}
		}

		readTheRows(true);

		long exeNano = System.nanoTime() - startNano;
		executionTimeMicros = (int)exeNano/1000;
		
		if (autoFetchProfiling){

			autoFetchManager.collectQueryInfo(autoFetchParentNode, autoFetchOriginQueryPoint, loadedBeanCount, executionTimeMicros);	
		}
		
		queryPlan.executionTime(loadedBeanCount, executionTimeMicros);
		
		return collection;
	}

	@SuppressWarnings("unchecked")
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
				transactionContext.clear();

			} else {
				// add to the list/set/map
				help.add(collection, getLoadedBean(), mapKey);
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

	private boolean navigateFirst() throws SQLException {
		if (query.getFirstRow() > 0 && !rset.absolute(query.getFirstRow())) {
			// firstRow has moved us beyond the end of the resultSet
			return false;
		}
		return true;
	}

	public String getLoadedRowDetail() {
		if (!manyIncluded) {
			return String.valueOf(rowCount);
		} else {
			return loadedBeanCount + ":" + rowCount;
		}
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
	public BeanPropertyAssocMany getManyProperty() {
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

	public ServerTransaction getTransaction() {
		return request.getTransaction();
	}

	public String getBeanType() {
		return desc.getFullName();
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
	
	public ObjectGraphNode createAutoFetchNode(String extra, JoinNode joinNode) {
		String path = joinNode.getPropertyPrefix();
		if (path == null){
			path = extra;
		} else {
			if (extra != null){
				path = path+"."+extra;				
			}
		}
		String beanIndex = String.valueOf(loadedBeanCount);
		return query.createObjectGraphNode(beanIndex, path);
	}
	
	public void profileReference(EntityBeanIntercept ebi, String extraPath) {
		profileBean(false, ebi, extraPath, currentJoinNode);		
	}
	
	public void profileBean(EntityBeanIntercept ebi, String extraPath, JoinNode joinNode) {
		profileBean(true, ebi, extraPath, joinNode);
	}
	
	private void profileBean(boolean bean, EntityBeanIntercept ebi, String extraPath, JoinNode joinNode) {
		
		ObjectGraphNode node = createAutoFetchNode(extraPath, joinNode);
		
		ebi.setNodeUsageCollector(new NodeUsageCollector(bean, node, autoFetchManager));
	}
	
	public void setCurrentJoinNode(JoinNode currentJoinNode){
		this.currentJoinNode = currentJoinNode;
	}
	
	public JoinNode getCurrentJoinNode(){
		return currentJoinNode;
	}
}
