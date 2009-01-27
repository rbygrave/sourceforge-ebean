package com.avaje.ebean.server.autofetch;

import java.io.Serializable;

import com.avaje.ebean.meta.AutoFetchQueryStats;

/**
 * Used to accumulate query execution statistics.
 */
public class StatisticsQuery implements Serializable {
	
	private static final long serialVersionUID = -1133958958072778811L;

	final String path;
	
	int exeCount;
	
	int totalBeanLoaded;
	
	int totalMicros;
	
	public StatisticsQuery(String path){
		this.path = path;
	}
		
	public AutoFetchQueryStats createPublicMeta() {
		return new AutoFetchQueryStats(path, exeCount, totalBeanLoaded, totalMicros);
	}
	
	public void add(int beansLoaded, int micros) {
		exeCount++;
		totalBeanLoaded += beansLoaded;
		totalMicros += micros;
	}
	
	public String toString() {
		long avgMicros = exeCount == 0 ? 0 : totalMicros / exeCount;
		
		return	"queryExe path["+path+"] count[" + exeCount + "] totalBeansLoaded[" + totalBeanLoaded + "] avgMicros["
					+ avgMicros + "] totalMicros[" + totalMicros + "]";
	}
}