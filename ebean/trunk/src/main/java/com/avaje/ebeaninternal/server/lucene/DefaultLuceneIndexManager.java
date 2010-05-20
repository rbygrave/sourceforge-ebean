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

import org.apache.lucene.analysis.Analyzer;

import com.avaje.ebeaninternal.api.SpiEbeanServer;

public class DefaultLuceneIndexManager {

    private final ConcurrentHashMap<String, LIndex> indexMap;
    private final ConcurrentHashMap<String, LIndex> indexByTypeAndName;
    
    private final Analyzer defaultAnalyzer;
    
    private final String baseDir;
    
    private SpiEbeanServer server;
    
    public DefaultLuceneIndexManager(Analyzer defaultAnalyzer, String baseDir, String serverName) {
        this.defaultAnalyzer = defaultAnalyzer;
        this.baseDir = baseDir + File.separator + serverName + File.separator;
        this.indexByTypeAndName = new ConcurrentHashMap<String, LIndex>();
        this.indexMap = new ConcurrentHashMap<String, LIndex>();
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
        indexMap.put(index.getDefnName(), index);
        
        String key = index.getBeanType().getName()+":"+null;
        indexByTypeAndName.put(key, index);
    }

    public LIndex getIndexByTypeAndName(Class<?> beanType, String name){
        String key = beanType.getName()+":"+name;
        return indexByTypeAndName.get(key);
    }
    
    public String getIndexDirectory(String indexName) {
        return baseDir + indexName;
    }
    
}
