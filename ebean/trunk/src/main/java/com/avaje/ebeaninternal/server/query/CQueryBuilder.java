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
package com.avaje.ebeaninternal.server.query;

import java.util.Iterator;
import java.util.Set;

import javax.persistence.PersistenceException;

import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.RawSql;
import com.avaje.ebean.RawSqlBuilder;
import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.RawSql.ColumnMapping;
import com.avaje.ebean.RawSql.ColumnMapping.Column;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.SqlLimitRequest;
import com.avaje.ebean.config.dbplatform.SqlLimitResponse;
import com.avaje.ebean.config.dbplatform.SqlLimiter;
import com.avaje.ebean.text.PathProperties;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.lucene.LIndex;
import com.avaje.ebeaninternal.server.lucene.LuceneIndexManager;
import com.avaje.ebeaninternal.server.persist.Binder;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryDetail;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryLimitRequest;

/**
 * Generates the SQL SELECT statements taking into account the physical
 * deployment properties.
 */
public class CQueryBuilder implements Constants {


	private final String tableAliasPlaceHolder;
	private final String columnAliasPrefix;
	
	private final SqlLimiter sqlLimiter;
	
	private final RawSqlSelectClauseBuilder sqlSelectBuilder;
    private final CQueryBuilderRawSql rawSqlHandler;

	private final Binder binder;
	
	private final BackgroundExecutor backgroundExecutor;
	
	private final boolean postgresPlatform;
	
	private final boolean luceneAvailable;
	
	private final UseIndex defaultUseIndex;
	
	/**
	 * Create the SqlGenSelect.
	 */
	public CQueryBuilder(BackgroundExecutor backgroundExecutor, DatabasePlatform dbPlatform, Binder binder, 
	        LuceneIndexManager luceneIndexManager) {
	
	    this.luceneAvailable = luceneIndexManager.isLuceneAvailable();
	    this.defaultUseIndex = luceneIndexManager.getDefaultUseIndex();
		this.backgroundExecutor = backgroundExecutor;
		this.binder = binder;
		this.tableAliasPlaceHolder = GlobalProperties.get("ebean.tableAliasPlaceHolder","${ta}");
		this.columnAliasPrefix = GlobalProperties.get("ebean.columnAliasPrefix", "c");
		this.sqlSelectBuilder = new RawSqlSelectClauseBuilder(dbPlatform, binder);

		this.sqlLimiter = dbPlatform.getSqlLimiter();
		this.rawSqlHandler = new CQueryBuilderRawSql(sqlLimiter);
		
		this.postgresPlatform = dbPlatform.getName().toLowerCase().indexOf("postgres") > -1;
	}

	protected String getOrderBy(String orderBy, BeanPropertyAssocMany<?> many, BeanDescriptor<?> desc,
			boolean hasListener) {

		String manyOrderBy = null;

		if (many != null) {
			manyOrderBy = many.getFetchOrderBy();
			if (manyOrderBy != null) {
				manyOrderBy = prefixOrderByFields(many.getName(), manyOrderBy);
			}
		}
		if (orderBy == null && (hasListener || manyOrderBy != null)) {
			// build orderBy to be the list of primary key columns
			StringBuffer sb = new StringBuffer();
			
			BeanProperty[] uids = desc.propertiesId();
			for (int i = 0; i < uids.length; i++) {				
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(uids[i].getName());
			}
			orderBy = sb.toString();
		}
		if (manyOrderBy != null) {
			// add first orderBy to manyOrderby
			orderBy = orderBy + " , " + manyOrderBy;
		}
		return orderBy;
	}

	/**
	 * split the order by claus on the field delimiter and prefix each field with the relation name
	 */
	public static String prefixOrderByFields(String name, String orderBy) {
		StringBuilder sb = new StringBuilder();
		for (String token : orderBy.split(",")) {
			if (sb.length() > 0) {
				sb.append(", ");
			}

			sb.append(name);
			sb.append(".");
			sb.append(token.trim());
		}

		return sb.toString();
	}

	/**
	 * Build the row count query.
	 */
	public <T> CQueryFetchIds buildFetchIdsQuery(OrmQueryRequest<T> request) {

    	SpiQuery<T> query = request.getQuery();
    	
    	query.setSelectId();
    	
		CQueryPredicates predicates = new CQueryPredicates(binder, request);
		CQueryPlan queryPlan = request.getQueryPlan();
		if (queryPlan != null){
		    // skip building the SqlTree and Sql string
	        predicates.prepare(false);
	        String sql = queryPlan.getSql();
	        return new CQueryFetchIds(request, predicates, sql, backgroundExecutor);
		    
		}
		
		String sql;
		if (isLuceneSupported(request) && predicates.isLuceneResolvable()){
		    //FIXME: CQueryFetchIds via Lucene
		    SqlTree sqlTree = createLuceneSqlTree(request, predicates);
	        queryPlan = new CQueryPlanLucene(request, sqlTree);
	        sql = "Lucene Index";
		
		} else {
		    // use RawSql or generated Sql
    		predicates.prepare(true);
    
    		SqlTree sqlTree = createSqlTree(request, predicates);
    		SqlLimitResponse s = buildSql(null, request, predicates, sqlTree);
    		sql = s.getSql();
    		
    	    // cache the query plan
            queryPlan = new CQueryPlan(sql, sqlTree, false, s.isIncludesRowNumberColumn(), predicates.getLogWhereSql());
		}
		
        request.putQueryPlan(queryPlan);
		return new CQueryFetchIds(request, predicates, sql, backgroundExecutor);
	}
	
	/**
	 * Build the row count query.
	 */
	public <T> CQueryRowCount buildRowCountQuery(OrmQueryRequest<T> request) {

    	SpiQuery<T> query = request.getQuery();

    	// always set the order by to null for row count query
    	query.setOrder(null);
    	
    	boolean hasMany = !query.getManyWhereJoins().isEmpty();
    	
    	query.setSelectId();
    	    	
    	String sqlSelect = "select count(*)";
    	if (hasMany){
    		// need to count distinct id's ...
        	query.setDistinct(true);
    		sqlSelect = null;
    	}
    	
		CQueryPredicates predicates = new CQueryPredicates(binder, request);
		CQueryPlan queryPlan = request.getQueryPlan();
		if (queryPlan != null){
	        // skip building the SqlTree and Sql string
	        predicates.prepare(false);
	        String sql = queryPlan.getSql();
	        return new CQueryRowCount(request, predicates, sql);
		}
		
		predicates.prepare(true);

		SqlTree sqlTree = createSqlTree(request, predicates);
		SqlLimitResponse s = buildSql(sqlSelect, request, predicates, sqlTree);
		String sql = s.getSql();
		if (hasMany){
			sql = "select count(*) from ( "+sql+")";
			if (postgresPlatform){
			    sql += " as c";
			}
		}
		
		// cache the query plan
        queryPlan = new CQueryPlan(sql, sqlTree, false, s.isIncludesRowNumberColumn(), predicates.getLogWhereSql());
		request.putQueryPlan(queryPlan);
		
		return new CQueryRowCount(request, predicates, sql);
	}
	
	private boolean isLuceneSupported(OrmQueryRequest<?> request) {
        if (!luceneAvailable){
            return false;
        }
        
        UseIndex useIndex = request.getQuery().getUseIndex();
        if (useIndex == null){
            // get the default strategy for this bean type
            useIndex = request.getBeanDescriptor().getUseIndex();
            if (useIndex == null){
                // get the default global strategy 
                useIndex = defaultUseIndex;                
            }
        }

        if (UseIndex.NO.equals(useIndex)){
            return false;
        }
        
        return true;
	}
	
	/**
	 * Return the SQL Select statement as a String. Converts logical property
	 * names to physical deployment column names.
	 */
	public <T> CQuery<T> buildQuery(OrmQueryRequest<T> request) {

		if (request.isSqlSelect()){
			return sqlSelectBuilder.build(request);
		}
		
		CQueryPredicates predicates = new CQueryPredicates(binder, request);
		
		CQueryPlan queryPlan = request.getQueryPlan();
		if (queryPlan != null){
			// Reuse the query plan so skip generating SqlTree and SQL.
			// We do prepare and bind the new parameters
			predicates.prepare(false);
			return new CQuery<T>(request, predicates, queryPlan);
		}

		if (isLuceneSupported(request) && predicates.isLuceneResolvable()){
		    // Use Lucene Index to resolve query
	        SqlTree sqlTree = createLuceneSqlTree(request, predicates);
            queryPlan = new CQueryPlanLucene(request, sqlTree);
		                
		} else {
		    // RawSql or Generated Sql query
		    
    		// Prepare the where, having and order by clauses. 
    		// This also parses them from logical property names to
    		// database columns and determines 'includes'. 
    		
    		// We need to check these 'includes' for extra joins 
    		// that are not included via select
    		predicates.prepare(true);
    
    		// Build the tree structure that represents the query.
    		SqlTree sqlTree = createSqlTree(request, predicates);
    		SqlLimitResponse res = buildSql(null, request, predicates, sqlTree);
    		
    		boolean rawSql = request.isRawSql();
    		if (rawSql){
                queryPlan = new CQueryPlanRawSql(request, res, sqlTree, predicates.getLogWhereSql());
    
    		} else {
    	        queryPlan = new CQueryPlan(request, res, sqlTree, rawSql, predicates.getLogWhereSql(), null);
    		}
		}		
		
		// cache the query plan because we can reuse it and also 
		// gather query performance statistics based on it.
		request.putQueryPlan(queryPlan);
		
		return new CQuery<T>(request, predicates, queryPlan);
	}
	
    /**
     * Build the SqlTree.
     * <p>
     * The SqlTree is immutable after construction and
     * so is safe to use by concurrent threads.
     * </p>
     * <p>
     * The predicates is used to add additional joins that come from
     * the where or order by clauses that are not already included for
     * the select clause.
     * </p>
     */
    private SqlTree createSqlTree(OrmQueryRequest<?> request, CQueryPredicates predicates) {

        if (request.isRawSql()){
            return createRawSqlSqlTree(request, predicates);
        }
        
        return new SqlTreeBuilder(tableAliasPlaceHolder, columnAliasPrefix, request, predicates).build();
    }

    private SqlTree createLuceneSqlTree(OrmQueryRequest<?> request, CQueryPredicates predicates) {

        LIndex luceneIndex = request.getLuceneIndex();
        OrmQueryDetail ormQueryDetail = luceneIndex.getOrmQueryDetail();
        
        
        // build SqlTree based on OrmQueryDetail of the LuceneIndex
        return new SqlTreeBuilder(request, predicates, ormQueryDetail).build();
    }
    
    private SqlTree createRawSqlSqlTree(OrmQueryRequest<?> request, CQueryPredicates predicates) {
        
        BeanDescriptor<?> descriptor = request.getBeanDescriptor();
        ColumnMapping columnMapping = request.getQuery().getRawSql().getColumnMapping();
        
        PathProperties pathProps = new PathProperties();
        
        // convert list of columns into (tree like) PathProperties
        Iterator<Column> it = columnMapping.getColumns();
        while (it.hasNext()) {
            RawSql.ColumnMapping.Column column = it.next();
            String propertyName = column.getPropertyName();
            if (!RawSqlBuilder.IGNORE_COLUMN.equals(propertyName)){
                
                ElPropertyValue el = descriptor.getElGetValue(propertyName);
                if (el == null){
                    String msg = "Property ["+propertyName+"] not found on "+descriptor.getFullName();
                    throw new PersistenceException(msg);
                }
                BeanProperty beanProperty = el.getBeanProperty();
                if (beanProperty.isId()){
                    // For @Id properties we chop off the last part of the path
                    propertyName = SplitName.parent(propertyName);
                } else if (beanProperty instanceof BeanPropertyAssocOne<?>) {
                    String msg = "Column ["+column.getDbColumn()+"] mapped to complex Property["+propertyName+"]";
                    msg += ". It should be mapped to a simple property (proably the Id property). ";
                    throw new PersistenceException(msg);                    
                }
                if (propertyName != null){
                    String[] pathProp = SplitName.split(propertyName);
                    pathProps.addToPath(pathProp[0], pathProp[1]);
                }
            }
        }
        
        OrmQueryDetail detail = new OrmQueryDetail();
        
        // transfer PathProperties into OrmQueryDetail
        Iterator<String> pathIt = pathProps.getPaths().iterator();
        while (pathIt.hasNext()) {
            String path = pathIt.next();
            Set<String> props = pathProps.get(path);
            detail.getChunk(path, true).setDefaultProperties(null, props);
        }

        // build SqlTree based on OrmQueryDetail of the RawSql
        return new SqlTreeBuilder(request, predicates, detail).build();
    }
    
	private SqlLimitResponse buildSql(String selectClause, OrmQueryRequest<?> request, CQueryPredicates predicates, SqlTree select) {
				
		SpiQuery<?> query = request.getQuery();
		
        RawSql rawSql = query.getRawSql();
        if (rawSql != null) {
            return rawSqlHandler.buildSql(request, predicates, rawSql.getSql());
        }
		
		BeanPropertyAssocMany<?> manyProp = select.getManyProperty();

		boolean useSqlLimiter = false;
		
		StringBuilder sb = new StringBuilder(500);

		if (selectClause != null){
			sb.append(selectClause);
			
		} else {

			useSqlLimiter = (query.hasMaxRowsOrFirstRow() && manyProp == null);
	
			if (!useSqlLimiter){
				sb.append("select ");
				if (query.isDistinct()) {
					sb.append("distinct ");
				}
			}
	
			sb.append(select.getSelectSql());
		}

		sb.append(" ").append(NEW_LINE);
		sb.append("from ");

		// build the from clause potentially with joins
		// required only for the predicates
		sb.append(select.getFromSql());

		String inheritanceWhere = select.getInheritanceWhereSql();
		
		boolean hasWhere = false;
		if (inheritanceWhere.length() > 0) {
			sb.append(" ").append(NEW_LINE).append("where");
			sb.append(inheritanceWhere);
			hasWhere = true;
		}
		
		if (request.isFindById() || query.getId() != null){
			if (hasWhere){
				sb.append(" and ");
			} else {
				sb.append(NEW_LINE).append("where ");
			}
			
			BeanDescriptor<?> desc = request.getBeanDescriptor();
			String idSql = desc.getIdBinderIdSql();
			sb.append(idSql).append(" ");
			hasWhere = true;
		}

		String dbWhere = predicates.getDbWhere();
		if (!isEmpty(dbWhere)) {
			if (!hasWhere) {
			    hasWhere = true;
				sb.append(" ").append(NEW_LINE).append("where ");
			} else {
				sb.append("and ");
			}
			sb.append(dbWhere);
		}
		
		String dbFilterMany = predicates.getDbFilterMany();
        if (!isEmpty(dbFilterMany)) {
            if (!hasWhere) {
                sb.append(" ").append(NEW_LINE).append("where ");
            } else {
                sb.append("and ");
            }
            sb.append(dbFilterMany);
        }

		String dbOrderBy = predicates.getDbOrderBy();
		if (dbOrderBy != null) {
			sb.append(" ").append(NEW_LINE);
			sb.append("order by ").append(dbOrderBy);
		}

		if (useSqlLimiter){
			// use LIMIT/OFFSET, ROW_NUMBER() or rownum type SQL query limitation
			SqlLimitRequest r = new OrmQueryLimitRequest(sb.toString(), dbOrderBy, query);
			return sqlLimiter.limit(r);
			
		} else {

			return new SqlLimitResponse(sb.toString(), false);
		}
		
	}

	private boolean isEmpty(String s){
	    return s == null || s.length() == 0;
	}

}
