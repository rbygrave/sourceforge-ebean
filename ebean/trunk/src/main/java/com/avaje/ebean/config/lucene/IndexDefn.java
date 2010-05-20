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
package com.avaje.ebean.config.lucene;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;

public interface IndexDefn<T> {
    
    public void initialise(IndexDefnBuilder helper);
    
    /**
     * Return the name of the default query field.
     */
    public String getDefaultField();
    
    public List<IndexFieldDefn> getFields();
        
    public boolean isUpdateSinceSupported();
    
    public String[] getUpdateSinceProperties();
    
    public Analyzer getAnalyzer(); 
    
    public MaxFieldLength getMaxFieldLength();
    
    public int getMaxBufferedDocs();
    
    public double getRAMBufferSizeMB();
    
    public int getTermIndexInterval();
    
    // Similarity
    // useCompoundFile
    // MergePolicy
}
