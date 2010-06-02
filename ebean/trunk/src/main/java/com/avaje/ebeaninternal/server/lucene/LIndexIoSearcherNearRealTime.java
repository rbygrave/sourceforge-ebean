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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;

public class LIndexIoSearcherNearRealTime implements LIndexIoSearcher {

    private static final Logger logger = Logger.getLogger(LIndexIoSearcherNearRealTime.class.getName());
    
    private final IndexWriter indexWriter;
    
    private volatile IndexSearcher indexSearcher;
    
    public LIndexIoSearcherNearRealTime(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
    }

    
    /**
     * Not used for Near Real Time.
     */
    public void postCommit() {
        
    }
    public IndexSearcher getIndexSearcher() {

        IndexSearcher s = internalGetIndexSearcher();
        s.getIndexReader().incRef();
        return s;
    }
    
    private IndexSearcher internalGetIndexSearcher() {
        
        IndexSearcher s = this.indexSearcher;
        try {
            if (s != null && s.getIndexReader().isCurrent()){
                return s;
            }
        } catch (IOException e){
            String msg = "Error checking IndexReader().isCurrent()";
            logger.log(Level.SEVERE, msg, e);
        }
        return createNewIndexSearcher();
    }
    
    private IndexSearcher createNewIndexSearcher() {
        try {            
            IndexReader r = indexWriter.getReader();
            IndexSearcher s = new IndexSearcher(r);
            this.indexSearcher = s;
            
            return s;
                    
        } catch (IOException e){
            String msg = "Fatal error getting Lucene IndexReader for "+indexWriter.getDirectory();
            throw new PersistenceLuceneException(msg, e);
        }
    }
}
