package org.avaje.ebean.server.autofetch;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.avaje.ebean.bean.ObjectGraphOrigin;
import org.avaje.ebean.bean.NodeUsageCollector;
import org.avaje.ebean.bean.ObjectGraphNode;
import org.avaje.ebean.meta.AutoFetchNodeUsageStats;
import org.avaje.ebean.meta.AutoFetchQueryStats;
import org.avaje.ebean.meta.MetaAutoFetchStatistic;
import org.avaje.ebean.query.OrmQueryDetail;
import org.avaje.ebean.server.deploy.jointree.JoinNode;

public class Statistics implements Serializable {


	private static final long serialVersionUID = -5586783791097230766L;

	final ObjectGraphOrigin origin;

	int counter;
	
	Map<String, StatisticsQuery> queryStatsMap = new LinkedHashMap<String, StatisticsQuery>();

	Map<String, StatisticsNodeUsage> nodeUsageMap = new LinkedHashMap<String, StatisticsNodeUsage>();

	final String monitor = new String();

	public Statistics(ObjectGraphOrigin origin) {
		this.origin = origin;
	}
	
	public ObjectGraphOrigin getOrigin() {
		return origin;
	}

	public TunedQueryInfo createTunedFetch(OrmQueryDetail newFetchDetail) {
		synchronized (monitor) {
			// NB: create a copy of queryPoint allowing garbage
			// collection of source...
			return new TunedQueryInfo(origin, newFetchDetail, counter);
		}
	}
	
	public MetaAutoFetchStatistic createPublicMeta() {
		
		synchronized (monitor) {
			
			StatisticsQuery[] sourceQueryStats = queryStatsMap.values().toArray(new StatisticsQuery[queryStatsMap.size()]);
			AutoFetchQueryStats[] destQueryStats = new AutoFetchQueryStats[sourceQueryStats.length];
			
			// copy the query statistics
			for (int i = 0; i < sourceQueryStats.length; i++) {
				destQueryStats[i] = sourceQueryStats[i].createPublicMeta();
			}
			
			StatisticsNodeUsage[] sourceNodeUsage = nodeUsageMap.values().toArray(new StatisticsNodeUsage[nodeUsageMap.size()]);
			AutoFetchNodeUsageStats[] destNodeUsage = new AutoFetchNodeUsageStats[sourceNodeUsage.length];

			// copy the node usage statistics
			for (int i = 0; i < sourceNodeUsage.length; i++) {
				destNodeUsage[i] = sourceNodeUsage[i].createPublicMeta();
			}
			
			return new MetaAutoFetchStatistic(origin, counter, destQueryStats, destNodeUsage);
		}
	}
	
	/**
	 * Return the number of times the root query has executed.
	 * <p>
	 * This tells us how much profiling we have done for this query.
	 * For example, after 100 times we may stop collecting more profiling info.
	 * </p>
	 */
	public int getCounter() {
		return counter;
	}
	
	public OrmQueryDetail buildTunedFetch(JoinNode joinRoot){
		
		synchronized (monitor) {
			
			OrmQueryDetail detail = new OrmQueryDetail();
			
			Iterator<StatisticsNodeUsage> it = nodeUsageMap.values().iterator();
			while (it.hasNext()) {
				StatisticsNodeUsage statsNode = (StatisticsNodeUsage) it.next();
				statsNode.buildTunedFetch(detail, joinRoot);
			}
			
			return detail;
		}
	}

	
	public void collectQueryInfo(ObjectGraphNode parentNode, int beansLoaded, int micros) {
		
		synchronized (monitor) {
			String key;
			if (parentNode == null){
				key = "";
				// this is basically the number of times the root query
				// has executed which gives us an indication of how
				// much profiling information we have gathered.
				counter++;
			} else {
				key = parentNode.getPath();
			}
			
			StatisticsQuery stats = queryStatsMap.get(key);
			if (stats == null){
				stats = new StatisticsQuery(key);
				queryStatsMap.put(key, stats);
			}
			stats.add(beansLoaded, micros);
		}
	}


	/**
	 * Collect the usage information for from a instance for this node.
	 */
	public void collectUsageInfo(NodeUsageCollector profile) {

		ObjectGraphNode node = profile.getNode();

		StatisticsNodeUsage nodeStats = getNodeStats(node.getPath());
		nodeStats.publish(profile);
	}

	private StatisticsNodeUsage getNodeStats(String path) {
		
		synchronized (monitor) {
			StatisticsNodeUsage nodeStats = nodeUsageMap.get(path);
			if (nodeStats == null) {
				nodeStats = new StatisticsNodeUsage(path);
				nodeUsageMap.put(path, nodeStats);
			}
			return nodeStats;
		}
	}

	public String toString() {

		synchronized (monitor) {
			StringBuilder sb = new StringBuilder();
			sb.append("\norigin[").append(origin).append("]");
			sb.append(" counter[").append(counter).append("]");

			for (StatisticsQuery queryStat : queryStatsMap.values()) {
				sb.append("\n").append(queryStat.toString());
			}		
		
			for (StatisticsNodeUsage node : nodeUsageMap.values()) {
				sb.append("\n").append(node.toString());
			}
			return sb.toString();
		}
	}

}
