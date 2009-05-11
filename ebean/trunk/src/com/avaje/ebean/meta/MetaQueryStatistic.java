package com.avaje.ebean.meta;

import java.io.Serializable;

import javax.persistence.Entity;

import com.avaje.ebean.bean.ObjectGraphOrigin;

/**
 * Query execution statistics Meta data.
 */
@Entity
public class MetaQueryStatistic implements Serializable {

	private static final long serialVersionUID = -8746524372894472584L;

	boolean autofetchTuned;
	
	/**
	 * Information defining the origin of the query (Call Stack and original query plan hash).
	 */
	ObjectGraphOrigin objectGraphOrigin;
	
	String beanType;

	/**
	 * The original query plan hash (calculated prior to autofetch tuning).
	 */
	int origQueryPlanHash;
	
	/**
	 * The final query plan hash (calculated prior to autofetch tuning).
	 */
	int finalQueryPlanHash;
	
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
	public MetaQueryStatistic(boolean autofetchTuned, ObjectGraphOrigin objectGraphOrigin, String beanType, int plan, String sql, 
			int executionCount, int totalLoadedBeans, int totalTimeMicros, long collectionStart) {
		
		this.autofetchTuned = autofetchTuned;
		this.objectGraphOrigin = objectGraphOrigin;
		this.origQueryPlanHash = objectGraphOrigin == null ? 0 : objectGraphOrigin.getQueryPlanHash();
		this.beanType = beanType;
		this.finalQueryPlanHash = plan;
		this.sql = sql;
		this.executionCount = executionCount;
		this.totalLoadedBeans = totalLoadedBeans;
		this.totalTimeMicros = totalTimeMicros;
		this.collectionStart = collectionStart;
	}

	public String toString() {
		return "type="+beanType+" tuned:"+autofetchTuned+" origHash="+origQueryPlanHash+" count="+executionCount+" avgMicros="+getAvgTimeMicros();
	}
	
	/**
	 * Return true if this query plan was built for Autofetch tuned queries.
	 */
	public boolean isAutofetchTuned() {
		return autofetchTuned;
	}
	
	/**
	 * If tuned via Autofetch this returns the origin point for the query (Call stack and 
	 * original queryPlan) and otherwise returns null.
	 */
	public ObjectGraphOrigin getObjectGraphOrigin() {
		return objectGraphOrigin;
	}

	/**
	 * Return the original query plan hash (calculated prior to autofetch tuning).
	 * <p>
	 * This will return 0 if there is no autofetch profiling or tuning on this query.
	 * </p>
	 */
	public int getOrigQueryPlanHash() {
		return origQueryPlanHash;
	}
	
	/**
	 * Return the queryPlanHash value. This is unique for a given query plan.
	 */
	public int getFinalQueryPlanHash() {
		return finalQueryPlanHash;
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
	 * Return the time collection started as a Date.
	 */
	public java.util.Date getCollectionStartDate() {
		return new java.util.Date(collectionStart);
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
