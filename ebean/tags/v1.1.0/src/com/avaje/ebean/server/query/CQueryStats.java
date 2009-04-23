
package com.avaje.ebean.server.query;

import com.avaje.ebean.meta.MetaQueryStatistic;

public class CQueryStats {

	private final int count;

	private final int totalLoadedBeanCount;

	private final int totalTimeMicros;

	private final long startCollecting;
	
	public CQueryStats() {
		count = 0;
		totalLoadedBeanCount = 0;
		totalTimeMicros = 0;
		startCollecting = System.currentTimeMillis();
	}

	public CQueryStats(CQueryStats previous, int loadedBeanCount, int timeMicros) {
		count = previous.count + 1;
		totalLoadedBeanCount = previous.totalLoadedBeanCount + loadedBeanCount;
		totalTimeMicros = previous.totalTimeMicros + timeMicros;
		startCollecting = previous.startCollecting;
	}

	public CQueryStats add(int loadedBeanCount, int timeMicros) {
		return new CQueryStats(this, loadedBeanCount, timeMicros);
	}

	public int getCount() {
		return count;
	}

	public int getAverageTimeMicros() {
		if (count == 0) {
			return 0;
		} else {
			return totalTimeMicros / count;
		}
	}
	
	public int getTotalLoadedBeanCount() {
		return totalLoadedBeanCount;
	}

	public int getTotalTimeMicros() {
		return totalTimeMicros;
	}

	public long getStartCollecting() {
		return startCollecting;
	}
	
	public MetaQueryStatistic createMetaQueryStatistic(String beanName, CQueryPlan qp) {
		return new MetaQueryStatistic(beanName, qp.getHash(), qp.getSql(), count, totalLoadedBeanCount, totalTimeMicros, startCollecting);
	}

}