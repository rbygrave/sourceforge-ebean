package org.avaje.ebean.meta;

import javax.persistence.Entity;

/**
 * Query execution statistics Meta data.
 */
@Entity
public class MetaQueryStatistic {

	String beanType;
	
	int queryPlanHash;
	
	String sql;
	
	int executionCount;
	
	int totalLoadedBeans;
	
	int totalTimeMicros;
	
	long collectionStart;
	
	public MetaQueryStatistic() {
		
	}
	
	/**
	 * Create a MetaQueryStatistic.
	 */
	public MetaQueryStatistic(String beanType, int plan, String sql, int executionCount, int totalLoadedBeans, int totalTimeMicros, long collectionStart) {
		this.beanType = beanType;
		this.queryPlanHash = plan;
		this.sql = sql;
		this.executionCount = executionCount;
		this.totalLoadedBeans = totalLoadedBeans;
		this.totalTimeMicros = totalTimeMicros;
		this.collectionStart = collectionStart;
	}

	public String toString() {
		return "type="+beanType+" plan="+queryPlanHash+" count="+executionCount+" avgMicros="+getAvgTimeMicros();
	}
	
	/**
	 * Return the queryPlanHash value. This is unique for a given query plan.
	 */
	public int getQueryPlanHash() {
		return queryPlanHash;
	}

	/**
	 * Return the bean type.
	 */
	public String getBeanType() {
		return beanType;
	}

	/**
	 * Return the sql executed.
	 */
	public String getSql() {
		return sql;
	}

	/**
	 * Return the total number of queries executed.
	 */
	public int getExecutionCount() {
		return executionCount;
	}

	/**
	 * Return the total number of beans loaded by the queries.
	 * <p>
	 * This excludes background fetching.
	 * </p>
	 */
	public int getTotalLoadedBeans() {
		return totalLoadedBeans;
	}

	/**
	 * Return the number of times this query was executed.
	 */
	public int getTotalTimeMicros() {
		return totalTimeMicros;
	}

	/**
	 * Return the time collection started.
	 */
	public long getCollectionStart() {
		return collectionStart;
	}
	
	/**
	 * Return the average query execution time in microseconds.
	 * <p>
	 * This excludes background fetching.
	 * </p>
	 */
	public int getAvgTimeMicros() {
		if (executionCount == 0){
			return 0;
		} else {
			return totalTimeMicros/executionCount;
		}
	}
	
	/**
	 * Return the average number of bean loaded per query.
	 * <p>
	 * This excludes background fetching.
	 * </p>
	 */
	public int getAvgLoadedBeans() {
		if (executionCount == 0){
			return 0;
		} else {
			return totalLoadedBeans/executionCount;
		}
	}
	
}
