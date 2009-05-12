package com.avaje.ebean.server.autofetch;

import java.io.Serializable;

import com.avaje.ebean.bean.ObjectGraphOrigin;
import com.avaje.ebean.meta.MetaAutoFetchTunedQueryInfo;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.query.OrmQueryDetail;

/**
 * Holds tuned query information. Is immutable so this represents the tuning at
 * a given point in time.
 */
public class TunedQueryInfo implements Serializable {

	private static final long serialVersionUID = 7381493228797997282L;

	final ObjectGraphOrigin origin;

	/**
	 * The tuned query details with joins and properties.
	 */
	OrmQueryDetail tunedDetail;

	/**
	 * The number of times profiling has been collected for this query point.
	 */
	int profileCount;
	
	Long lastTuneTime = Long.valueOf(0);

	final String rateMonitor = new String();

	final String countMonitor = new String();

	/**
	 * The number of queries tuned by this object.
	 * Could use AtomicInteger perhaps.
	 */
	transient int tunedCount;

	transient int rateTotal;

	transient int rateHits;

	transient double lastRate;

	public TunedQueryInfo(ObjectGraphOrigin queryPoint, OrmQueryDetail tunedDetail, int profileCount) {
		this.origin = queryPoint;
		this.tunedDetail = tunedDetail;
		this.profileCount = profileCount;
	}

	/**
	 * Return true if this query should be profiled based on a percentage rate.
	 */
	public boolean isPercentageProfile(double rate) {
		
		synchronized (rateMonitor) {

			if (lastRate != rate) {
				// the rate has changed so resetting
				lastRate = rate;
				rateTotal = 0;
				rateHits = 0;
			}

			rateTotal++;
			if (rate > (double) rateHits / rateTotal) {
				rateHits++;
				return true;
			} else {
				return false;
			}
		}
	}

	/**
	 * Create a copy of this tuned fetch data for public consumption.
	 */
	public MetaAutoFetchTunedQueryInfo createPublicMeta() {
		return new MetaAutoFetchTunedQueryInfo(origin, tunedDetail.toString(), profileCount, tunedCount, lastTuneTime);
	}

	/**
	 * Set the number of times profiling has been collected for this query
	 * point.
	 */
	public void setProfileCount(int profileCount) {
		// int assignment is atomic
		this.profileCount = profileCount;
	}

	/**
	 * Set the tuned query detail.
	 */
	public void setTunedDetail(OrmQueryDetail tunedDetail) {
		// assignment is atomic
		this.tunedDetail = tunedDetail;
		this.lastTuneTime = new Long(System.currentTimeMillis());
	}

	/**
	 * Return true if the fetches are essentially the same.
	 */
	public boolean isSame(OrmQueryDetail newQueryDetail) {
		if (tunedDetail == null) {
			return false;
		}
		return tunedDetail.queryPlanHash() == newQueryDetail.queryPlanHash();
	}

	/**
	 * Tune the query by replacing its OrmQueryDetail with a tuned one.
	 * 
	 * @return true if the query was tuned, otherwise false.
	 */
	public boolean autoFetchTune(OrmQuery<?> query) {
		if (tunedDetail != null) {
			//Note: tunedDetail is immutable by convention
			query.setDetail(tunedDetail);	
			query.setAutoFetchTuned(true);
			synchronized (countMonitor) {
				// a case for AtomicInteger
				tunedCount++;	
			}
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Return the time of the last tune.
	 */
	public Long getLastTuneTime() {
		return lastTuneTime;
	}

	/**
	 * Return the number of queries tuned by this object.
	 */
	public int getTunedCount() {
		return tunedCount;
	}

	/**
	 * Return the number of times profiling has been collected for this query
	 * point.
	 */
	public int getProfileCount() {
		return profileCount;
	}

	public OrmQueryDetail getTunedDetail() {
		return tunedDetail;
	}

	public ObjectGraphOrigin getOrigin() {
		return origin;
	}

	public String getLogOutput(OrmQueryDetail newQueryDetail) {
		
		boolean changed = newQueryDetail != null;
		
		StringBuilder sb = new StringBuilder(150);
		sb.append( changed ? "\"Changed\",":"\"New\",");
		sb.append("\"").append(origin.getBeanType()).append("\",");
		sb.append("\"").append(origin.getKey()).append("\",");
		if (changed){
			sb.append("\"to: ").append(newQueryDetail.toString()).append("\",");
			sb.append("\"from: ").append(tunedDetail.toString()).append("\",");
		} else {
			sb.append("\"to: ").append(tunedDetail.toString()).append("\",");			
			sb.append("\"\",");
		}
		sb.append("\"").append(origin.getFirstStackElement()).append("\"");
		
		return sb.toString();
	}
	
	public String toString() {
		return origin.getBeanType()+" "+origin.getKey()+" " + tunedDetail;
	}

}
