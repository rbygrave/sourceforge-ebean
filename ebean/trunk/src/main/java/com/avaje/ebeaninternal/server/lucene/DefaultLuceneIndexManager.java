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
package com.avaje.ebeaninternal.server.lucene;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;

import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.config.lucene.IndexDefn;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.cluster.LuceneClusterIndexSync;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.transaction.IndexEvent;
import com.avaje.ebeaninternal.server.transaction.RemoteTransactionEvent;

public class DefaultLuceneIndexManager implements LuceneIndexManager, Runnable {

    private static final Logger logger = Logger.getLogger(DefaultLuceneIndexManager.class.getName());
    
    private final ConcurrentHashMap<String, LIndex> indexMap;
    
    private final ClusterManager clusterManager;
    
    private final LuceneClusterIndexSync clusterIndexSync;
    
    private final BackgroundExecutor backgroundExecutor;
    
    private final Analyzer defaultAnalyzer;
    
    private final String baseDir;
    
    private final LIndexFactory indexFactory;

    private final boolean luceneAvailable;
    
    private final UseIndex defaultUseIndex;
    
    private final String serverName;
    
    private SpiEbeanServer server;
    
    private Thread thread;
    
    private volatile boolean shutdown;

    private volatile boolean shutdownComplete;

    private long manageFreqMillis = 100;
     
    /**
     * Construct when Lucene is available.
     */
    public DefaultLuceneIndexManager(ClusterManager clusterManager, BackgroundExecutor backgroundExecutor, 
            Analyzer defaultAnalyzer, String baseDir, String serverName, UseIndex defaultUseIndex) {
        
        this.luceneAvailable = true;
        this.serverName = serverName;
        this.clusterManager = clusterManager;
        this.clusterIndexSync = clusterManager.getLuceneClusterIndexSync();
        this.backgroundExecutor = backgroundExecutor;
        this.defaultUseIndex = defaultUseIndex;
        this.defaultAnalyzer = defaultAnalyzer;
        this.baseDir = baseDir + File.separator + serverName + File.separator;
        this.indexMap = new ConcurrentHashMap<String, LIndex>();
        this.indexFactory = new LIndexFactory(this);
        
        this.thread = new Thread(this, "Ebean-"+serverName+"-LuceneManager");
    }
    
    public void notifyCluster(IndexEvent event) {
        
        if (clusterIndexSync != null && clusterIndexSync.isMaster()) {
            // we are the master, notify the slaves 
            logger.info("-- notifyCluster commit ... ");
            RemoteTransactionEvent e = new RemoteTransactionEvent(serverName);
            e.addIndexEvent(event);
            clusterManager.broadcast(e);
        }
    }

    protected void execute(LIndexSync indexSync){
        if (clusterIndexSync != null){
            IndexSynchRun r = new IndexSynchRun(clusterIndexSync, indexSync);
            backgroundExecutor.execute(r);
        }
    }
    
    public void processEvent(IndexEvent indexEvent) {
        
        if (clusterIndexSync == null){
            return;
        }
        
        String masterHost = clusterIndexSync.getMasterHost();
        if (masterHost == null) {
            logger.warning("Master got IndexEvent "+indexEvent+" ?");
            
        } else {
            String idxName = indexEvent.getIndexName();
            if (idxName != null){
                LIndex index = getIndex(idxName);
                if (index == null){
                    logger.warning("Can't find Lucene Index ["+idxName+"]");
                } else {
                    index.queueSync(masterHost);
                }
            }
        }
    }

    public void processEvent(RemoteTransactionEvent txnEvent, SpiTransaction localTransaction) {
        
        Collection<IndexUpdates> events = IndexUpdatesBuilder.create(server, txnEvent);
        for (IndexUpdates e : events) {
            BeanDescriptor<?> beanDescriptor = e.getBeanDescriptor();
            LIndex luceneIndex = beanDescriptor.getLuceneIndex();
            if (luceneIndex != null){
                LIndexUpdateFuture future = luceneIndex.process(e);
                if (localTransaction != null){
                    localTransaction.addIndexUpdateFuture(future);
                }
            }
        }
        
    }

    public LuceneClusterIndexSync getClusterIndexSync() {
        return clusterIndexSync;
    }

    public boolean isLuceneAvailable() {
        return luceneAvailable;
    }
    
    public UseIndex getDefaultUseIndex() {
        return defaultUseIndex;
    }

    public LIndex create(IndexDefn<?> indexDefn, BeanDescriptor<?> descriptor) throws IOException {
        return indexFactory.create(indexDefn, descriptor);
    }
    
    public SpiEbeanServer getServer() {
        return server;
    }

    public void setServer(SpiEbeanServer server) {
        this.server = server;
    }

    public Analyzer getDefaultAnalyzer() {
        return defaultAnalyzer;
    }

    public void addIndex(LIndex index) throws IOException {
        synchronized (indexMap) {
            indexMap.put(index.getName(), index);
        }
    }

    public LIndex getIndex(String name){
        return indexMap.get(name);
    }
    
    public String getIndexDirectory(String indexName) {
        return baseDir + indexName;
    }  
    
    public void start() {
        this.thread.setDaemon(true);
        this.thread.start();
        logger.info("Lucene Manager started");
    }
    
    public void shutdown() {
        
        shutdown = true;
        synchronized (thread) {
            try {
                // wait max 20 seconds 
                thread.wait(20000);                
            } catch (InterruptedException e) {
                logger.info("InterruptedException:"+e);
            }
        }
        
        if (!shutdownComplete){
            String msg = "WARNING: Shutdown of Lucene Manager did not complete?";
            System.err.println(msg);
            logger.warning(msg);
        }
    }
        
    private void fireOnStartup() {
        if (clusterIndexSync != null && !clusterIndexSync.isMaster()){
            String masterHost = clusterIndexSync.getMasterHost();
            if (masterHost != null){
                for (LIndex index : indexMap.values()) {
                    index.queueSync(masterHost);
                }
            }
        }
    }
    
    public void run() {
        
        fireOnStartup();
        
        while (!shutdown) {
            synchronized (indexMap) {
                long start = System.currentTimeMillis();
                for (LIndex idx : indexMap.values()) {
                    idx.manage(this);
                }
                long exeTime = System.currentTimeMillis() - start;
                long sleepMillis = manageFreqMillis - exeTime;
                if (sleepMillis > 0){
                    try {
                        Thread.sleep(sleepMillis);
                    } catch (InterruptedException e) {
                        logger.log(Level.INFO,"Interrupted", e);
                    }
                }
            }
        }
        shutdownComplete = true;
        synchronized (thread) {
            thread.notifyAll();
        }
    }
    
    
    private static class IndexSynchRun implements Runnable {
        
        private final LuceneClusterIndexSync clusterIndexSync;
        private final LIndex index;
        private final String masterHost;
        
        private IndexSynchRun(LuceneClusterIndexSync clusterIndexSync, LIndexSync indexSync) {
            this.clusterIndexSync = clusterIndexSync;
            this.index = indexSync.getIndex();
            this.masterHost = indexSync.getMasterHost();
        }
        
        public void run() {
            boolean success = false;
            try {
                clusterIndexSync.sync(index, masterHost);
                success = true;
            } catch (IOException e) {
                String msg = "Failed to sync Lucene index "+index;
                logger.log(Level.SEVERE, msg, e);
            } finally {
                index.syncFinished(success);
            }
        }
    }
}
