package com.avaje.ebean.meta;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.avaje.ebean.bean.ObjectGraphOrigin;

/**
 * Statistics collected by AutoFetch profiling.
 */
@Entity
public class MetaAutoFetchStatistic implements Serializable {

	private static final long serialVersionUID = -6640406753257176803L;

	@Id
	String id;
	
	ObjectGraphOrigin origin;

	int counter;
	
	@Transient
	List<AutoFetchQueryStats> queryStats;
	
	@Transient
	List<AutoFetchNodeUsageStats> nodeUsageStats;
	
	public MetaAutoFetchStatistic() {
	}
	
	public MetaAutoFetchStatistic(ObjectGraphOrigin origin, int counter, AutoFetchQueryStats[] queryStats, AutoFetchNodeUsageStats[] nodeUsageStats) {
		this();
		this.origin = origin;
		this.id = origin.getKey();
		this.counter = counter;
		this.queryStats = null;//new ArrayList(queryStats);
		this.nodeUsageStats = null;//nodeUsageStats;
	}

	/**
	 * This is the query point key.
	 */
	public String getId() {
		return id;
	}


	/**
	 * Return the query point.
	 */
	public ObjectGraphOrigin getOrigin() {
		return origin;
	}

	/**
	 * Return the number of profiled queries the statistics is based on.
	 */
	public int getCounter() {
		return counter;
	}

	/**
	 * Return the query execution statistics.
	 */
	public List<AutoFetchQueryStats> getQueryStats() {
		return queryStats;
	}

	/**
	 * Return the node usage statistics.
	 */
	public List<AutoFetchNodeUsageStats> getNodeUsageStats() {
		return nodeUsageStats;
	}
	
}
