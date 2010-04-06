/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebeaninternal.server.lib.thread;

import java.util.Hashtable;
import java.util.Iterator;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.server.lib.BackgroundThread;

/**
 * Singleton that manages a list of named ThreadPools.
 * <pre><code>
 * &#47;&#47; Create or get the runnable
 * Runnable runnable = ...;
 * 
 * &#47;&#47; Run the runnable in the background
 * ThreadPoolManager.assign("mypool",runnable,true);
 * </code></pre>
 */
public class ThreadPoolManager implements Runnable {

    private static final class Single {
    	private static final ThreadPoolManager me = new ThreadPoolManager();
    }
    
    private static int debugLevel = 0;

    /**
     * set when the pools are being shutdown.
     */
    private boolean isShuttingDown = false;

    /** 
     * Holds all the thread pools. 
     */
    private Hashtable<String,ThreadPool> threadPoolCache = new Hashtable<String, ThreadPool>();

    /**
     * Monitor used for threadPoolCache.
     */
    private Object cacheMonitor = new Object();

    /**
     * The default time threads are idle before they are stopped and removed.
     * This can occur when the pool grows larger than the min size and then goes idle
     * for some time.
     */
    private long defaultIdleTime;

    private ThreadPoolManager() {
        initialise();
    }

    private void initialise() {

        debugLevel = GlobalProperties.getInt("threadpool.debugLevel", 0);
        
        defaultIdleTime = 1000 * GlobalProperties.getInt("threadpool.idletime", 60);

        int freqIsSecs = GlobalProperties.getInt("threadpool.sleeptime", 30);

        BackgroundThread.add(freqIsSecs, this);
    }

    /**
     * Set the debug level.
     */
    public static void setDebugLevel(int level) {
        debugLevel = level;
    }

    /**
     * Return the debug level.
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

//    /**
//     * Return the singleton instance.
//     */
//    private static ThreadPoolManager getInstance() {
//        return ThreadPoolManagerHolder.me;
//    }

    /**
     * Periodically maintains the pool size.  Stops threads that have
     * been idle for too long and ensures the minimum number of threads.
     * <p>
     * To change this you can set the threadpool.idletime property:<br>
     * <br>
     * <b><code>## set threadpool idletime to 120 seconds</code></b><br>
     * <b><code>threadpool.idletime=120</code></b><br>
     * </p>
     */
    public void run() {
        if (!isShuttingDown) {
            maintainPoolSize();
        }
    }

    /**
     * Assign a Runnable to a named ThreadPool. Returns true if the work is
     * going to be run immediately.
     * <p>
     * If the pool has reached its maxium size and...<br>
     * addToQueueIfFull == true -&gt; queue the work and return false<br>
     * addToQueueIfFull == false -&gt; don't queue the work and return false<br>
     * </p>
     * @param poolName the name of the thread pool to perform the work
     * @param work the work to be run
     * @param addToQueueIfFull if pool is full indicates to queue or reject the work
     */
    public static boolean assign(String poolName, Runnable work, boolean addToQueueIfFull) {
        return getThreadPool(poolName).assign(work, addToQueueIfFull);
    }

    /**
     * Return the named thread pool.
     */
    public static ThreadPool getThreadPool(String poolName) {
        return Single.me.getPool(poolName);
    }

    /**
     * Return the named ThreadPool. If the ThreadPool doesn't exist it will be
     * created.
     */
    private ThreadPool getPool(String poolName) {
        ThreadPool threadPool = (ThreadPool) threadPoolCache.get(poolName);
        if (threadPool == null) {
            synchronized (cacheMonitor) {
                threadPool = (ThreadPool) threadPoolCache.get(poolName);
                if (threadPool == null) {
                    threadPool = createThreadPool(poolName);
                    threadPoolCache.put(poolName, threadPool);
                }
            }
        }
        return threadPool;
    }

    /**
     * Returns an iterator of ThreadPools.
     * <p>
     * Note that the ThreadPools should not be removed by the iterator.
     * </p>
     */
    public static Iterator<ThreadPool> pools() {
        return Single.me.threadPoolCache.values().iterator();
    }

    /**
     * Maintain the size of all the thread pools.
     * Trims down to minimum size threads that have been idle for a while.
     * Adds threads if it is short of the minimum size.
     */
    private void maintainPoolSize() {
        if (isShuttingDown){
            return;
        }
        synchronized (cacheMonitor) {
   
            Iterator<ThreadPool> e = pools();
            while (e.hasNext()) {
                ThreadPool pool = (ThreadPool) e.next();
                pool.maintainPoolSize();
            }
        }
    }

    /**
     * Shutdown all the ThreadPools nicely.
     * This will wait for all currently runnable and queued work to finish.
     */
    public static void shutdown() {
    	Single.me.shutdownPools();
    }

    private void shutdownPools() {
        synchronized (cacheMonitor) {
            isShuttingDown = true;
            Iterator<ThreadPool> i = pools();
            while (i.hasNext()) {
                ThreadPool pool = (ThreadPool) i.next();
                pool.shutdown();
            }
        }
    }

    private ThreadPool createThreadPool(String poolName) {

        int min = GlobalProperties.getInt("threadpool." + poolName + ".min", 0);
        int max = GlobalProperties.getInt("threadpool." + poolName + ".max", 100);

        long idle = 1000 * GlobalProperties.getInt("threadpool." + poolName + ".idletime", -1);
        if (idle < 0) {
            idle = defaultIdleTime;
        }

        boolean isDaemon = true;
        Integer priority = null;
        String threadPriority = GlobalProperties.get("threadpool." + poolName + ".priority", null);
        if (threadPriority != null) {
            priority = new Integer(threadPriority);
        }

        ThreadPool newPool = new ThreadPool(poolName, isDaemon, priority);
        newPool.setMaxSize(max);
        newPool.setMinSize(min);
        newPool.setMaxIdleTime(idle);

        return newPool;
    }

};
