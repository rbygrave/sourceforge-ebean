package com.avaje.ebean.meta;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.avaje.ebean.bean.ObjectGraphOrigin;

/**
 * "Tuned fetch" information used by AutoFetch.
 * <p>
 * Note that the queryPoint is effectively the Id field for this bean.
 * </p>
 * <p>
 * The queryPoint identifies both the query and call stack.
 * </p>
 */
@Entity
public class MetaAutoFetchTunedQueryInfo implements Serializable {

	private static final long serialVersionUID = 3119991928889170217L;

	@Id
	final String id;
	
	final int origQueryPlanHash;
	
	/**
	 * The profile query point (call stack and query).
	 */
	final ObjectGraphOrigin origin;

	/**
	 * The tuned query details with joins and properties.
	 */
	final String tunedDetail;

	/**
	 * The number of times profiling has been collected for this query point.
	 */
	final int profileCount;
	
	/**
	 * The number of queries tuned by this info.
	 */
	final int tunedCount;

	public MetaAutoFetchTunedQueryInfo() {
		
		this.origin = null;
		this.origQueryPlanHash = 0;
		this.id = null;
		this.tunedDetail = null;
		this.profileCount = 0;
		this.tunedCount = 0;
	}
	
	public MetaAutoFetchTunedQueryInfo(final ObjectGraphOrigin origin, String tunedDetail,
			int profileCount, int tunedCount) {
		
		this.origin = origin;
		this.origQueryPlanHash = origin == null ? 0 : origin.getQueryPlanHash();
		this.id = origin.getKey();
		this.tunedDetail = tunedDetail;
		this.profileCount = profileCount;
		this.tunedCount = tunedCount;
	}

	/**
	 * Return the query point key.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Return the original query plan hash (calculated prior to autofetch tuning).
	 */
	public int getOrigQueryPlanHash() {
		return origQueryPlanHash;
	}

	/**
	 * Return the query point.
	 */
	public ObjectGraphOrigin getOrigin() {
		return origin;
	}

	/**
	 * The tuned query detail in string form.
	 */
	public String getTunedDetail() {
		return tunedDetail;
	}

	/**
	 * The number of profiled queries the tuned query is based on.
	 */
	public int getProfileCount() {
		return profileCount;
	}
	
	/**
	 * Return the number of queries tuned.
	 */
	public int getTunedCount() {
		return tunedCount;
	}

	public String toString() {
		return "origin[" + origin + "] query[" + tunedDetail + "] profileCount["+ profileCount + "]";
	}
}
