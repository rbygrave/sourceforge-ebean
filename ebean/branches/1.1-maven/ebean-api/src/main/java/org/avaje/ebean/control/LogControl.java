package org.avaje.ebean.control;

/**
 * Management control of the logging levels.
 */
public interface LogControl {

	/**
	 * Level for no logging.
	 */
	public static final int LOG_NONE = 0;
	/**
	 * Level for logging summary information.
	 */
	public static final int LOG_SUMMARY = 1;
	/**
	 * Level for logging bind information.
	 */
	public static final int LOG_BIND = 2;
	/**
	 * Level for logging sql information.
	 */
	public static final int LOG_SQL = 3;

	/**
	 * The current log level for native sql queries.
	 */
	public int getSqlQueryLevel();

	/**
	 * Set the log level for native sql queries.
	 */
	public void setSqlQueryLevel(int sqlQueryLevel);

	/**
	 * The current log level for query using an id.
	 */
	public int getQueryByIdLevel();

	/**
	 * The current log level for FindMany.
	 */
	public int getQueryManyLevel();

	/**
	 * Return the log level for orm updates.
	 */
	public int getOrmUpdateLevel();

	/**
	 * Set the log level for orm updates.
	 */
	public void setOrmUpdateLevel(int ormUpdateLevel);

	/**
	 * The current log level for bean delete.
	 */
	public int getDeleteLevel();

	/**
	 * Set the log level for bean delete.
	 */
	public void setDeleteLevel(int deleteLevel);

	/**
	 * The current log level for bean insert.
	 */
	public int getInsertLevel();

	/**
	 * Set the log level for bean insert.
	 */
	public void setInsertLevel(int insertLevel);

	/**
	 * The current log level for bean update.
	 */
	public int getUpdateLevel();

	/**
	 * Set the log level for bean update.
	 */
	public void setUpdateLevel(int updateLevel);

	/**
	 * The current log level for CallableSql.
	 */
	public int getCallableSqlLevel();

	/**
	 * Set the current log level for CallableSql.
	 */
	public void setCallableSqlLevel(int callableSqlLevel);

	/**
	 * The current log level for UpdatableSql.
	 */
	public int getSqlUpdateLevel();

	/**
	 * Set the current log level for UpdatableSql.
	 */
	public void setSqlUpdateLevel(int sqlUpdateLevel);

	/**
	 * Set the log level for FindByUid.
	 */
	public void setQueryByIdLevel(int queryByIdLevel);

	/**
	 * Set the log level for FindMany.
	 */
	public void setQueryManyLevel(int queryManyLevel);

	/**
	 * If true Log generated sql to the console.
	 */
	public boolean isDebugGeneratedSql();

	/**
	 * Set to true to Log generated sql to the console.
	 */
	public void setDebugGeneratedSql(boolean debugSql);

	/**
	 * Return true if lazy loading should be debugged.
	 */
	public boolean isDebugLazyLoad();

	/**
	 * Set the debugging on lazy loading.
	 */
	public void setDebugLazyLoad(boolean debugLazyLoad);

}