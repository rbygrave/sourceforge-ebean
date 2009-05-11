package com.avaje.ebean.server.autofetch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import javax.persistence.PersistenceException;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.bean.CallStack;
import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.ObjectGraphOrigin;
import com.avaje.ebean.control.ImplicitAutoFetchMode;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.query.OrmQueryDetail;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.deploy.jointree.JoinNode;
import com.avaje.ebean.server.deploy.jointree.JoinTree;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * The manager of all the usage/query statistics as well as the tuned fetch
 * information.
 */
public class DefaultAutoFetchManager implements AutoFetchManager, Serializable {

	private static final String AVAJE_EBEAN = Ebean.class.getName().substring(0,15);

	private static final long serialVersionUID = -6826119882781771722L;

	private final String statisticsMonitor = new String();

	final String fileName;

	/**
	 * Map of the usage and query statistics gathered.
	 */
	Map<String, Statistics> statisticsMap = new ConcurrentHashMap<String, Statistics>();

	/**
	 * Map of the tuned query details per profile query point.
	 */
	Map<String, TunedQueryInfo> tunedQueryInfoMap = new ConcurrentHashMap<String, TunedQueryInfo>();

	transient long defaultGarbageCollectionWait = 100;

	/**
	 * Left without synchronized for now.
	 */
	transient int tunedQueryCount;
	
	/**
	 * Converted from a 0-100 int to a double. Effectively a percentage rate at
	 * which to collect profiling information.
	 */
	transient double profilingRate = 0.1d;

	transient int profilingBase = 10;

	transient int profilingMin = 1;

	transient boolean profiling;

	transient boolean queryTuning;

	transient ImplicitAutoFetchMode implicitAutoFetchMode;

	/**
	 * Server that owns this Profile Listener.
	 */
	transient InternalEbeanServer server;

	/**
	 * The logger.
	 */
	transient DefaultAutoFetchManagerLogging logging;

	public DefaultAutoFetchManager(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Set up this profile listener before it is active.
	 */
	public void setOwner(InternalEbeanServer server) {
		this.server = server;
		this.logging = new DefaultAutoFetchManagerLogging(server.getPlugin(), this);

		PluginProperties properties = server.getPlugin().getProperties();
		queryTuning = properties.getPropertyBoolean("autofetch.querytuning", false);
		profiling = properties.getPropertyBoolean("autofetch.profiling", false);
		profilingMin = properties.getPropertyInt("autofetch.profiling.min", 1);
		profilingBase = properties.getPropertyInt("autofetch.profiling.base", 10);

		String strRate = properties.getProperty("autofetch.profiling.rate", "0.05");
		try {
			double rate = Double.parseDouble(strRate);
			setProfilingRate(rate);
		} catch (Exception e){
			setProfilingRate(0.05d);
		}
		

		defaultGarbageCollectionWait = (long) properties.getPropertyInt(
			"autofetch.garbageCollectionWait", 100);

		// determine the mode to use when Query.setAutoFetch() was
		// not explicitly set
		String mode = properties.getProperty("autofetch.implicitmode",
			ImplicitAutoFetchMode.DEFAULT_ON_IF_EMPTY.name());
		implicitAutoFetchMode = ImplicitAutoFetchMode.valueOf(mode.toUpperCase());

		// log the guts of the autoFetch setup
		String msg = "AutoFetch queryTuning[" + queryTuning + "] profiling[" + profiling
				+ "] implicitMode[" + implicitAutoFetchMode + "]  profiling rate[" + profilingRate
				+ "] min[" + profilingMin + "] base[" + profilingBase + "]";

		logging.logToJavaLogger(msg);
	}

	
	
	public void clearQueryStatistics() {
		server.clearQueryStatistics();
	}

	/**
	 * Return the number of queries tuned by AutoFetch.
	 */
	public int getTotalTunedQueryCount(){
		return tunedQueryCount;
	}
	
	/**
	 * Return the size of the TuneQuery map.
	 */
	public int getTotalTunedQuerySize(){
		return tunedQueryInfoMap.size();
	}
	
	/**
	 * Return the size of the profile map.
	 */
	public int getTotalProfileSize(){
		return statisticsMap.size();
	}
	
	public int clearTunedQueryInfo() {
		
		// reset the rough count as well
		tunedQueryCount = 0;
		
		// clear the map...
		int size = tunedQueryInfoMap.size();
		tunedQueryInfoMap.clear();
		return size;
	}

	public int clearProfilingInfo() {
		int size = statisticsMap.size();
		statisticsMap.clear();
		return size;
	}

	
	public void serialize() {

		File autoFetchFile = new File(fileName);

		try {
			FileOutputStream fout = new FileOutputStream(autoFetchFile);

			ObjectOutputStream oout = new ObjectOutputStream(fout);
			oout.writeObject(this);
			oout.flush();
			oout.close();

		} catch (Exception e) {
			String msg = "Error serializing autofetch file";
			logging.logError(Level.SEVERE, msg, e);
		}
	}

	/**
	 * Return the current Tuned query info for a given origin key.
	 */
	public TunedQueryInfo getTunedQueryInfo(String originKey) {
		return tunedQueryInfoMap.get(originKey);
	}

	/**
	 * Return the current Statistics for a given originKey key.
	 */
	public Statistics getStatistics(String originKey) {
		return statisticsMap.get(originKey);
	}

	public Iterator<TunedQueryInfo> iterateTunedQueryInfo() {
		return tunedQueryInfoMap.values().iterator();
	}

	public Iterator<Statistics> iterateStatistics() {
		return statisticsMap.values().iterator();
	}

	public boolean isProfiling() {
		return profiling;
	}

	/**
	 * When the application is running, BEFORE turning off profiling you
	 * probably should call collectUsageViaGC() as there is a delay (waiting for
	 * garbage collection) collecting usage profiling information.
	 */
	public void setProfiling(boolean profiling) {
		this.profiling = profiling;
	}

	public boolean isQueryTuning() {
		return queryTuning;
	}

	public void setQueryTuning(boolean queryTuning) {
		this.queryTuning = queryTuning;
	}

	public ImplicitAutoFetchMode getImplicitAutoFetchMode() {
		return implicitAutoFetchMode;
	}

	public void setImplicitAutoFetchMode(ImplicitAutoFetchMode implicitAutoFetchMode) {
		this.implicitAutoFetchMode = implicitAutoFetchMode;
	}

	public double getProfilingRate() {
		return profilingRate;
	}

	public void setProfilingRate(double rate) {
		if (rate < 0) {
			rate = 0d;
		} else if (rate > 1) {
			rate = 1d;
		}
		profilingRate = rate;
	}

	public int getProfilingBase() {
		return profilingBase;
	}

	public void setProfilingBase(int profilingBase) {
		this.profilingBase = profilingBase;
	}

	public int getProfilingMin() {
		return profilingMin;
	}

	public void setProfilingMin(int profilingMin) {
		this.profilingMin = profilingMin;
	}

	/**
	 * Shutdown the listener.
	 * <p>
	 * We should try to collect the usage statistics by calling a System.gc().
	 * This is necessary for use with short lived applications where garbage
	 * collection may not otherwise occur at all.
	 * </p>
	 */
	public void shutdown() {
		collectUsageViaGC(-1);
		serialize();
	}

	/**
	 * Ask for a System.gc() so that we gather node usage information.
	 * <p>
	 * Really only want to do this sparingly but useful just prior to shutdown
	 * for short run application where garbage collection may otherwise not
	 * occur at all.
	 * </p>
	 * <p>
	 * waitMillis will do a thread sleep to give the garbage collection a little
	 * time to do its thing assuming we are shutting down the VM.
	 * </p>
	 * <p>
	 * If waitMillis is -1 then the defaultGarbageCollectionWait is used which
	 * defaults to 100 milliseconds.
	 * </p>
	 */
	public String collectUsageViaGC(long waitMillis) {
		System.gc();
		try {
			if (waitMillis < 0) {
				waitMillis = defaultGarbageCollectionWait;
			}
			Thread.sleep(waitMillis);
		} catch (InterruptedException e) {
			String msg = "Error while sleeping after System.gc() request.";
			logging.logError(Level.SEVERE, msg, e);
			return msg;
		}
		return updateTunedQueryInfo();
	}

	/**
	 * Update the tuned fetch plans from the current usage information.
	 */
	public String updateTunedQueryInfo() {

		if (!profiling) {
			// we are not collecting any profiling information at
			// the moment so don't try updating the tuned query plans.
			return "Not profiling";
		}

		synchronized (statisticsMonitor) {

			int countNewPlan = 0;
			int countModified = 0;
			int countUnchanged = 0;

			Iterator<Statistics> it = statisticsMap.values().iterator();
			while (it.hasNext()) {
				Statistics queryPointStatistics = it.next();
				ObjectGraphOrigin queryPoint = queryPointStatistics.getOrigin();
				String beanType = queryPoint.getBeanType();

				try {

					Class<?> beanClass = Class.forName(beanType);
					BeanManager<?> beanMgr = server.getBeanManager(beanClass);
					if (beanMgr == null){
						// perhaps a entity as an inner class
						logging.logToJavaLogger("No BeanMgr for "+beanClass);
						
					} else {
						JoinTree beanJoinTree = beanMgr.getBeanJoinTree();
						JoinNode joinRoot = beanJoinTree.getRoot();
	
						// Determine the fetch plan from the latest statistics.
						// Use this to compare with current "tuned fetch plan".
						OrmQueryDetail newFetchDetail = queryPointStatistics.buildTunedFetch(joinRoot);
	
						// get the current tuned fetch info...
						TunedQueryInfo currentFetch = tunedQueryInfoMap.get(queryPoint.getKey());
	
						if (currentFetch == null) {
							// its a new fetch plan, add it.
							countNewPlan++;
	
							currentFetch = queryPointStatistics.createTunedFetch(newFetchDetail);
							logging.logNew(currentFetch);
							tunedQueryInfoMap.put(queryPoint.getKey(), currentFetch);
	
						} else if (!currentFetch.isSame(newFetchDetail)) {
							// the fetch plan has changed, update it.
							countModified++;
							logging.logChanged(currentFetch, newFetchDetail);
							currentFetch.setTunedDetail(newFetchDetail);
	
						} else {
							// the fetch plan has not changed...
							countUnchanged++;
						}
	
						currentFetch.setProfileCount(queryPointStatistics.getCounter());
					}

				} catch (ClassNotFoundException e) {
					// expected after renaming/moving an entity bean
					String msg = e.toString()+" updating autoFetch tuned query for " + beanType
						+". It isLikely this bean has been renamed or moved";
					logging.logError(Level.INFO, msg, null);
					statisticsMap.remove(queryPointStatistics);
				}
			}

			Object[] a = new Object[] { countNewPlan, countModified, countUnchanged };
			String summaryInfo = String.format("new[%d] modified[%d] unchanged[%d]", a);

			if (countNewPlan > 0 || countModified > 0){
				// only log it if its interesting
				logging.logSummary(summaryInfo);
			}
			
			return summaryInfo;
		}
	}

	/**
	 * Return true if we should try to use autoFetch for this query.
	 */
	private boolean useAutoFetch(OrmQuery<?> query) {

		Boolean autoFetch = query.isAutoFetch();
		if (autoFetch != null) {
			// explicitly set...
			return autoFetch.booleanValue();

		} else {
			// determine using implicit mode...
			switch (implicitAutoFetchMode) {
			case DEFAULT_ON:
				return true;

			case DEFAULT_OFF:
				return false;

			case DEFAULT_ON_IF_EMPTY:
				return query.isDetailEmpty();

			default:
				throw new PersistenceException("Invalid autoFetchMode " + implicitAutoFetchMode);
			}
		}
	}

	/**
	 * Auto tune the query and enable profiling.
	 */
	public void tuneQuery(OrmQuery<?> query) {

		if (!queryTuning && !profiling) {
			return;
		}

		if (!useAutoFetch(query)) {
			// not using autoFetch for this query
			return;
		}

		ObjectGraphNode parentAutoFetchNode = query.getParentNode();
		if (parentAutoFetchNode != null) {
			// This is a lazy loading query with profiling on.
			// We continue to collect the profiling information.
			query.setAutoFetchManager(this);

			// TODO: Future feature - tune a lazy loading query?
			return;
		}

		// create a query point to identify the query
		CallStack stack = createCallStack();
		ObjectGraphOrigin queryPoint = query.createObjectGraphOrigin(stack);

		// get current "tuned fetch" for this query point
		TunedQueryInfo tunedFetch = tunedQueryInfoMap.get(queryPoint.getKey());

		// get the number of times we have collected profiling information
		int profileCount = tunedFetch == null ? 0 : tunedFetch.getProfileCount();

		if (queryTuning) {
			if (tunedFetch != null && profileCount >= profilingMin) {
				// deemed to have enough profiling 
				// information for automatic tuning
				if (tunedFetch.autoFetchTune(query)){
					// tunedQueryCount++ not thread-safe, could use AtomicInteger.
					// But I'm happy if this statistic is a little wrong
					// and this is a VERY HOT method
					tunedQueryCount++;
				}
					
			}
		}

		if (profiling) {
			// we want more profiling information?
			if (tunedFetch == null) {
				query.setAutoFetchManager(this);

			} else if (profileCount < profilingBase) {
				query.setAutoFetchManager(this);

			} else if (tunedFetch.isPercentageProfile(profilingRate)) {
				query.setAutoFetchManager(this);
			}
		}
	}

	/**
	 * Gather query execution statistics. This could either be the originating
	 * query in which case the parentNode will be null, or a lazy loading query
	 * resulting from traversal of the object graph.
	 */
	public void collectQueryInfo(ObjectGraphNode parentNode, ObjectGraphOrigin queryPoint,
			int beans, int micros) {

		Statistics stats = getQueryPointStats(queryPoint);
		stats.collectQueryInfo(parentNode, beans, micros);
	}

	/**
	 * Collect usage statistics from a node in the object graph.
	 * <p>
	 * This is sent to use from a EntityBeanIntercept when the finalise method
	 * is called on the bean.
	 * </p>
	 */
	public void collectNodeUsage(NodeUsageCollector usageCollector) {

		if (usageCollector.isUnloadedReference()) {
			// a reference bean that was not lazy loaded
			// so no real interest in this yet...
			return;
		}

		ObjectGraphOrigin originQueryPoint = usageCollector.getNode().getOriginQueryPoint();

		Statistics queryPointStats = getQueryPointStats(originQueryPoint);

		queryPointStats.collectUsageInfo(usageCollector);
	}

	private Statistics getQueryPointStats(ObjectGraphOrigin originQueryPoint) {
		synchronized (statisticsMonitor) {
			Statistics stats = statisticsMap.get(originQueryPoint.getKey());
			if (stats == null) {
				stats = new Statistics(originQueryPoint);
				statisticsMap.put(originQueryPoint.getKey(), stats);
			}
			return stats;
		}
	}

	public String toString() {
		synchronized (statisticsMonitor) {
			return statisticsMap.values().toString();
		}
	}

	static final int MAX_STACK_SIZE = 20;

	static final int IGNORE_LEADING_ELEMENTS = 6;

	/**
	 * Create a CallStack object.
	 * <p>
	 * This trims off the avaje ebean part of the stack trace so that 
	 * the first element in the CallStack should be application code.
	 * </p>
	 */
	public CallStack createCallStack() {

		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

		// ignore the first 6 as they are always avaje stack elements
		int startIndex = IGNORE_LEADING_ELEMENTS;
		
		// find the first non-avaje stackElement
		for (; startIndex < stackTrace.length; startIndex++) {
			if (!stackTrace[startIndex].getClassName().startsWith(AVAJE_EBEAN)) {
				break;
			}
		}
		
		int stackLength = stackTrace.length - startIndex;
		if (stackLength > 20) {
			// maximum of 20 stackTrace elements
			stackLength = 20;
		}
		
		// create the 'interesting' part of the stackTrace
		StackTraceElement[] finalTrace = new StackTraceElement[stackLength];
		for (int i = 0; i < stackLength; i++) {
			finalTrace[i] = stackTrace[i + startIndex];
		}

		if (stackLength < 1){
			// this should not really happen
			throw new RuntimeException("StackTraceElement size 0?  stack: "+Arrays.toString(stackTrace));
		}

		return new CallStack(finalTrace);
	}


}
