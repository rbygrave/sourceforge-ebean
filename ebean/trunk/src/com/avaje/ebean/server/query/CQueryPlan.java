package com.avaje.ebean.server.query;

import com.avaje.ebean.meta.MetaQueryStatistic;

/**
 * Represents a query for a given SQL statement.
 * <p>
 * This can be executed multiple times with different bind parameters.
 * </p>
 * <p>
 * That is, the sql including the where clause, order by clause etc must be
 * exactly the same to share the same query plan with the only difference being
 * bind values.
 * </p>
 * <p>
 * This is useful in that is common in OLTP type applications that the same
 * query will be executed quite a lot just with different bind values. With this
 * query plan we can bypass some of the query statement generation (for
 * performance) and collect statistics on the number and average execution
 * times. This is turn can be used to identify queries that could be looked at
 * for performance tuning.
 * </p>
 */
public class CQueryPlan {

	private final int hash;
	
	private final boolean rawSql;

	private final boolean rowNumberIncluded;

	private final String sql;

	private final String logWhereSql;

	private final SqlTree selectClause;

	private CQueryStats queryStats = new CQueryStats();

	public CQueryPlan(int hash, String sql, SqlTree selectClause, boolean rawSql, boolean rowNumberIncluded,
			String logWhereSql) {
		this.hash = hash;
		this.sql = sql;
		this.selectClause = selectClause;
		this.rawSql = rawSql;
		this.rowNumberIncluded = rowNumberIncluded;
		this.logWhereSql = logWhereSql;
	}

	public int getHash() {
		return hash;
	}

	public String getSql() {
		return sql;
	}

	public SqlTree getSelectClause() {
		return selectClause;
	}

	public boolean isRawSql() {
		return rawSql;
	}

	public boolean isRowNumberIncluded() {
		return rowNumberIncluded;
	}

	public String getLogWhereSql() {
		return logWhereSql;
	}

	/**
	 * Reset the query statistics.
	 */
	public void resetStatistics() {
		queryStats = new CQueryStats();
	}
	
	/**
	 * Register an execution time against this query plan;
	 */
	public void executionTime(int loadedBeanCount, int timeMicros) {
		queryStats = queryStats.add(loadedBeanCount, timeMicros);
	}

	/**
	 * Return the current query statistics.
	 */
	public CQueryStats getQueryStats() {
		return queryStats;
	}
	
	public MetaQueryStatistic createMetaQueryStatistic(String beanName) {
		return queryStats.createMetaQueryStatistic(beanName, this);
	}

}
