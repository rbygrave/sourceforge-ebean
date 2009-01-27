package com.avaje.ebean.query;

import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlQueryListener;
import com.avaje.ebean.util.BindParams;

/**
 * SQL query - Internal extension to SqlQuery.
 */
public interface RelationalQuery extends SqlQuery {

	/**
	 * Return the named or positioned parameters.
	 */
	public BindParams getBindParams();

	/**
	 * return the query.
	 */
	public String getQuery();

	/**
	 * Return the base table for the MapBeans.
	 */
	public String getBaseTable();
	
	/**
	 * Return the queryListener.
	 */
	public SqlQueryListener getListener();
	
	/**
	 * Return the first row to fetch.
	 */
    public int getFirstRow();
    
    /**
     * Return the maximum number of rows to fetch.
     */
	public int getMaxRows();
	
	/**
	 * Return the number of rows after which background fetching occurs.
	 */
	public int getBackgroundFetchAfter();
	
	/**
	 * Return the initial capacity.
	 */
	public int getInitialCapacity();
	
	/**
	 * Return the key property for maps.
	 */
	public String getMapKey();
	
	/**
	 * Return the query timeout.
	 */
	public int getTimeout();

}
