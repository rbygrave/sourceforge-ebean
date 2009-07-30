package com.avaje.ebean.meta;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import com.avaje.ebean.bean.ObjectGraphOrigin;

/**
 * Statistics collected by AutoFetch profiling.
 */
@Entity
public class MetaAutoFetchStatistic implements Serializable {

	private static final long serialVersionUID = -6640406753257176804L;

	@Id
	private String id;
	
	private ObjectGraphOrigin origin;

	private int origQueryPlanHash;
	
	private String beanType;
	
	private int counter;
	
	@Transient
	private List<QueryStats> queryStats;
	
	@Transient
	private List<NodeUsageStats> nodeUsageStats;
	
	public MetaAutoFetchStatistic() {
	}
	
	public MetaAutoFetchStatistic(ObjectGraphOrigin origin, int counter, List<QueryStats> queryStats, List<NodeUsageStats> nodeUsageStats) {
		
		this.origin = origin;
		this.origQueryPlanHash = origin == null ? 0 : origin.getQueryPlanHash();
		this.beanType = origin == null ? null : origin.getBeanType();
		this.id = origin == null ? null : origin.getKey();
		this.counter = counter;
		this.queryStats = queryStats;
		this.nodeUsageStats = nodeUsageStats;
	}

	/**
	 * This is the query point key.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * Return the bean type.
	 */
	public String getBeanType() {
		return beanType;
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
	 * Return the number of profiled queries the statistics is based on.
	 */
	public int getCounter() {
		return counter;
	}

	/**
	 * Return the query execution statistics.
	 */
	public List<QueryStats> getQueryStats() {
		return queryStats;
	}

	/**
	 * Return the node usage statistics.
	 */
	public List<NodeUsageStats> getNodeUsageStats() {
		return nodeUsageStats;
	}
	
	public static class QueryStats implements Serializable {

		private static final long serialVersionUID = -5517935732867671387L;

		final String path;

		final int exeCount;

		final int totalBeanLoaded;

		final int totalMicros;

		public QueryStats(String path, int exeCount, int totalBeanLoaded, int totalMicros) {
			this.path = path;
			this.exeCount = exeCount;
			this.totalBeanLoaded = totalBeanLoaded;
			this.totalMicros = totalMicros;
		}

		/**
		 * Return the path. This is empty string for the origin query and otherwise
		 * the path for the associated lazy loading queries.
		 */
		public String getPath() {
			return path;
		}

		/**
		 * The number of queries executed.
		 */
		public int getExeCount() {
			return exeCount;
		}

		/**
		 * The total number of beans loaded by the query.
		 */
		public int getTotalBeanLoaded() {
			return totalBeanLoaded;
		}

		/**
		 * The total time in microseconds of the queries.
		 */
		public int getTotalMicros() {
			return totalMicros;
		}

		public String toString() {
			long avgMicros = exeCount == 0 ? 0 : totalMicros / exeCount;

			return "queryExe path[" + path + "] count[" + exeCount + "] totalBeansLoaded["
					+ totalBeanLoaded + "] avgMicros[" + avgMicros + "] totalMicros[" + totalMicros
					+ "]";
		}
	}
	
	/**
	 * Collects usages statistics for a given node in the object graph.
	 */
	public static class NodeUsageStats implements Serializable {

		private static final long serialVersionUID = 1786787832374844739L;

		final String path;

		final int loadCount;

		final int usedCount;

		final String[] usedProperties;

		public NodeUsageStats(String path, int loadCount, int usedCount, String[] usedProperties) {
			this.path = path == null ? "" : path;
			this.loadCount = loadCount;
			this.usedCount = usedCount;
			this.usedProperties = usedProperties;
		}

		/**
		 * Return the path. This is empty string for the origin and otherwise the
		 * path for the associated nodes.
		 */
		public String getPath() {
			return path;
		}

		/**
		 * The number of loaded beans for this node.
		 */
		public int getLoadCount() {
			return loadCount;
		}

		/**
		 * The number of beans actually used for this node.
		 * <p>
		 * The difference between loaded and used could show uneven traversal of the
		 * object graph. UI paging through results means the traversal for the first
		 * x beans can be much higher than the last x beans.
		 * </p>
		 */
		public int getUsedCount() {
			return usedCount;
		}

		/**
		 * The properties used at this node.
		 */
		public String[] getUsedProperties() {
			return usedProperties;
		}

		/**
		 * Return the properties as a Set rather than an Array.
		 */
		public Set<String> getUsedPropertiesSet() {
			LinkedHashSet<String> s = new LinkedHashSet<String>();
			for (int i = 0; i < usedProperties.length; i++) {
				s.add(usedProperties[i]);
			}
			return s;
		}
		
		public String toString() {
			return "path[" + path + "] load[" + loadCount + "] used[" + usedCount + "] props"+ Arrays.toString(usedProperties);
		}
	}
}
