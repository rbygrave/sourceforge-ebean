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

import java.io.IOException;

import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.config.lucene.IndexDefn;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.cluster.LuceneClusterIndexSync;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.transaction.IndexEvent;

/**
 * Used when Lucene is not in the class path.
 */
public class NoLuceneIndexManager implements LuceneIndexManager {

    
    public void start() {
    }
    
    public void shutdown() {        
    }

    public void setServer(SpiEbeanServer server) {
        
    }

    public boolean isLuceneAvailable() {
        return false;
    }
    
    public void processEvent(IndexEvent indexEvent) {
        
    }

    public LuceneClusterIndexSync getClusterIndexSync() {
        throw new RuntimeException("Never Called");
    }
    
    public void notifyCluster(IndexEvent event) {
        throw new RuntimeException("Never Called");
    }

    public void addIndex(LIndex index) throws IOException {
        throw new RuntimeException("Never Called");        
    }
    
    public LIndex getIndex(String defnName) {
        throw new RuntimeException("Never Called");
    }

    public LIndex create(IndexDefn<?> indexDefn, BeanDescriptor<?> descriptor) throws IOException {
        throw new RuntimeException("Never Called");
    }

    public UseIndex getDefaultUseIndex() {
        throw new RuntimeException("Never Called");
    }

    public LIndex getIndexByTypeAndName(Class<?> beanType, String name) {
        throw new RuntimeException("Never Called");
    }

    public String getIndexDirectory(String indexName) {
        throw new RuntimeException("Never Called");
    }

    public SpiEbeanServer getServer() {
        throw new RuntimeException("Never Called");
    }


    
}
