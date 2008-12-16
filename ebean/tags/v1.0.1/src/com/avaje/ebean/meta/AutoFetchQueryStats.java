package com.avaje.ebean.meta;

import java.io.Serializable;

/**
 * Used to accumulate query execution statistics.
 */
public class AutoFetchQueryStats implements Serializable {

	private static final long serialVersionUID = -5517935732867671387L;

	final String path;

	final int exeCount;

	final int totalBeanLoaded;

	final int totalMicros;

	public AutoFetchQueryStats(String path, int exeCount, int totalBeanLoaded, int totalMicros) {
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