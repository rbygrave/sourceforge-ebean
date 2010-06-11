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

public interface LuceneIndexManager {
    
    public void start();
    
    public void shutdown();
    
    public boolean isLuceneAvailable();
    
    public LuceneClusterIndexSync getClusterIndexSync();
    
    public void processEvent(IndexEvent indexEvent);

    public UseIndex getDefaultUseIndex();

    public LIndex create(IndexDefn<?> indexDefn, BeanDescriptor<?> descriptor) throws IOException;

    public LIndex getIndex(String defnName);

    public SpiEbeanServer getServer();

    public void setServer(SpiEbeanServer server);

    public void addIndex(LIndex index) throws IOException;

    public LIndex getIndexByTypeAndName(Class<?> beanType, String name);

    public String getIndexDirectory(String indexName);

    public void notifyCluster(IndexEvent event);

}