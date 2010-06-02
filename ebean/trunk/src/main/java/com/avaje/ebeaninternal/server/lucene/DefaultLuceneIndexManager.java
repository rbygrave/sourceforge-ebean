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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;

import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.config.lucene.IndexDefn;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

public class DefaultLuceneIndexManager implements LuceneIndexManager, Runnable {

    private static final Logger logger = Logger.getLogger(DefaultLuceneIndexManager.class.getName());
    
    private final ConcurrentHashMap<String, LIndex> indexMap;
    private final ConcurrentHashMap<String, LIndex> indexByTypeAndName;
    
    private final Analyzer defaultAnalyzer;
    
    private final String baseDir;
    
    private final LIndexFactory indexFactory;

    private final boolean luceneAvailable;
    
    private final UseIndex defaultUseIndex;
    
    private SpiEbeanServer server;
    
    private Thread thread;
    
    /**
     * Construct when Lucene is available.
     */
    public DefaultLuceneIndexManager(Analyzer defaultAnalyzer, String baseDir, String serverName, UseIndex defaultUseIndex) {
        this.luceneAvailable = true;
        this.defaultUseIndex = defaultUseIndex;
        this.defaultAnalyzer = defaultAnalyzer;
        this.baseDir = baseDir + File.separator + serverName + File.separator;
        this.indexByTypeAndName = new ConcurrentHashMap<String, LIndex>();
        this.indexMap = new ConcurrentHashMap<String, LIndex>();
        this.indexFactory = new LIndexFactory(this);
        
        this.thread = new Thread(this, "EbeanLuceneManager");
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
            indexMap.put(index.getDefnName(), index);
            
            String key = index.getBeanType().getName()+":"+null;
            indexByTypeAndName.put(key, index);            
        }
    }

    public LIndex getIndexByTypeAndName(Class<?> beanType, String name){
        String key = beanType.getName()+":"+name;
        return indexByTypeAndName.get(key);
    }
    
    public String getIndexDirectory(String indexName) {
        return baseDir + indexName;
    }

    public void start() {
        this.thread.setDaemon(true);
        this.thread.start();
        logger.info("Lucene Manager started");
    }
    
    private volatile boolean shutdown;
    private volatile boolean shutdownComplete;

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
    
    private long manageFreqMillis = 100;
    
    public void run() {
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
    
}
