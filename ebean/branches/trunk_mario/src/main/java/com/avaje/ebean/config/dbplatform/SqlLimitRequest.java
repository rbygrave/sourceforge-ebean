package com.avaje.ebean.config.dbplatform;

import com.avaje.ebeaninternal.api.SpiQuery;

/**
 * The request object for the query that can have sql limiting 
 * applied to it (such as a LIMIT OFFSET clause).
 * 
 * @author rob
 */
public interface SqlLimitRequest {

	/**
	 * Return true if the query uses distinct.
	 */
	public boolean isDistinct();
	
	/**
	 * Return the first row value.
	 */
	public int getFirstRow();
	
	/**
	 * Return the max rows for this query.
	 */
	public int getMaxRows();
	
	/**
	 * Return the sql query.
	 */
	public String getDbSql();

	/**
	 * Return the orderBy clause of the sql query.
	 */
	public String getDbOrderBy();

    /**
     * return the query
     */
    public SpiQuery<?> getOrmQuery();

    /**
     * return the database platform
     */
    public DatabasePlatform getDbPlatform();
}
