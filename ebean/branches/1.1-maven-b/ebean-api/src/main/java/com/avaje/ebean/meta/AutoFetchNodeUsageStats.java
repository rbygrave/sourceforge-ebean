package com.avaje.ebean.meta;

import java.io.Serializable;

/**
 * Collects usages statistics for a given node in the object graph.
 */
public class AutoFetchNodeUsageStats implements Serializable {

	private static final long serialVersionUID = 1786787832374844739L;

	final String path;

	final int loadCount;

	final int usedCount;

	final String[] usedProperties;

	public AutoFetchNodeUsageStats(String path, int loadCount, int usedCount,
			String[] usedProperties) {
		this.path = path;
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

	public String toString() {
		return "path[" + path + "] load[" + loadCount + "] used[" + usedCount + "] props"
				+ usedProperties;
	}
}