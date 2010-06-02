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
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

/**
 * Used when Lucene is not in the class path.
 */
public class NoLuceneIndexManager implements LuceneIndexManager {

    public boolean isLuceneAvailable() {
        return false;
    }

    public void addIndex(LIndex index) throws IOException {
    }

    public LIndex create(IndexDefn<?> indexDefn, BeanDescriptor<?> descriptor) throws IOException {
        return null;
    }

    public UseIndex getDefaultUseIndex() {
        return null;
    }

    public LIndex getIndexByTypeAndName(Class<?> beanType, String name) {
        return null;
    }

    public String getIndexDirectory(String indexName) {
        return null;
    }

    public SpiEbeanServer getServer() {
        return null;
    }

    public void setServer(SpiEbeanServer server) {
        
    }

    
}