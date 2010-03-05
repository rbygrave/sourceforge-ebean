/**
 * Copyright (C) 2009 Authors
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebeaninternal.server.lib;


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebeaninternal.api.Monitor;

/**
 * The Thread Pool based on Daemon threads.
 * 
 * @author rbygrave
 */
public final class DaemonThreadPool extends ThreadPoolExecutor {

    private static final Logger logger = Logger.getLogger(DaemonThreadPool.class.getName());

    private final Monitor monitor = new Monitor();
    
    private int shutdownWaitSeconds;
    
	/**
	 * Construct the DaemonThreadPool.
	 * 
	 * @param coreSize
	 *            the core size of the thread pool.
	 * @param keepAliveSecs
	 *            the time in seconds idle threads are keep alive
	 * @param shutdownWaitSeconds
	 *            the time in seconds allowed for the pool to shutdown nicely.
	 *            After this the pool is forced to shutdown.
	 */
    public DaemonThreadPool(int coreSize, long keepAliveSecs, int shutdownWaitSeconds, String namePrefix) {
        super(coreSize, coreSize, keepAliveSecs, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new DaemonThreadFactory(namePrefix));
        this.shutdownWaitSeconds = shutdownWaitSeconds;
        // we want to shutdown nicely when either the web application stops.
        // Adding the JVM shutdown hook as a safety (and when not run in tomcat)
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    }

    /**
     * Shutdown this thread pool nicely if possible.
     * <p>
     * This will wait a maximum of 20 seconds before terminating any threads
     * still working.
     * </p>
     */
    public void shutdown() {
        synchronized (monitor) {
            if (super.isShutdown()) {
                logger.fine("... DaemonThreadPool already shut down");
                return;
            }
            try {
                logger.fine("DaemonThreadPool shutting down...");
                super.shutdown();
                if (!super.awaitTermination(shutdownWaitSeconds, TimeUnit.SECONDS)) {
                    logger.info("ScheduleService shut down timeout exceeded. Terminating running threads.");
                    super.shutdownNow();
                }

            } catch (Exception e) {
                String msg = "Error during shutdown";
                logger.log(Level.SEVERE, msg, e);
                e.printStackTrace();
            }
        }
    }

    /**
     * Fired by the JVM Runtime shutdown.
     */
    private class ShutdownHook extends Thread {
        @Override
        public void run() {
            shutdown();
        }
    };
}

