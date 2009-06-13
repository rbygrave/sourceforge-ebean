package com.avaje.ebean.config.dbplatform;


public interface SqlLimitRequest {

	
	public boolean isDistinct();
	
	/**
	 * Return the first row value.
	 */
	public int getFirstRow();
	
	/**
	 * Return the max rows for this query.
	 */
	public int getMaxRows();
	
	public String getDbSql();
	
	public String getDbOrderBy();
	
}
