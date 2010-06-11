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

import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;

public class LIndexIoSearcherNearRealTime implements LIndexIoSearcher {

    private static final Logger logger = Logger.getLogger(LIndexIoSearcherNearRealTime.class.getName());
    
    private final IndexWriter indexWriter;
    
    private volatile IndexSearcher indexSearcher;
    
    public LIndexIoSearcherNearRealTime(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
        this.indexSearcher = createDirectorySearcher();
    }

    public void postCommit() {
        try {
            IndexSearcher s = getIndexSearcher(true, true);
            s.getIndexReader().decRef();
        } catch (Exception e){
            String msg = "Error postCommit() refreshing IndexSearcher";
            logger.log(Level.SEVERE, msg, e);
        }
    }
    
    
    public void refresh(boolean nearRealTime) {
        try {
            getIndexSearcher(true, nearRealTime);
            //s.getIndexReader().decRef();
        } catch (Exception e){
            String msg = "Error postCommit() refreshing IndexSearcher";
            logger.log(Level.SEVERE, msg, e);
        }
    }
    
    public LIndexVersion getLastestVersion() {
        
        IndexSearcher s = getIndexSearcher();
        try {
            IndexCommit c = s.getIndexReader().getIndexCommit();
            return new LIndexVersion(c.getGeneration(), c.getVersion());
            
        } catch (IOException e) {
            throw new PersistenceLuceneException(e);
            
        } finally {
            try {
                s.getIndexReader().decRef();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error on IndexReader.decRef()", e);
            }
        }
    }
    
    public IndexSearcher getIndexSearcher() {
        
        return getIndexSearcher(false, true);
    }

    
    private IndexSearcher getIndexSearcher(boolean forceNew, boolean nearRealTime) {
        
        IndexSearcher s = this.indexSearcher;
        try {
            if (!forceNew && s != null) {
                IndexReader r = s.getIndexReader();
                if (r.getRefCount() > 0 && r.isCurrent()){
                    r.incRef();
                    return s;
                }
            }
        } catch (IOException e){
            String msg = "Error checking IndexReader.isCurrent()";
            logger.log(Level.SEVERE, msg, e);
        }
        
        IndexSearcher newSearcher = createDirectorySearcher();
        //IndexSearcher newSearcher = nearRealTime ? createNearRealTimeSearcher() : createDirectorySearcher();
        this.indexSearcher = newSearcher;
        
//        if (s != null){
//            try {
//                //IndexReader r = s.getIndexReader();
//                //if (r.)
//                //.decRef();
//            } catch (IOException e){
//                String msg = "Error checking IndexReader.decRef()";
//                logger.log(Level.SEVERE, msg, e);
//            }
//        }
        return newSearcher;
    }
    
//    private IndexSearcher createNearRealTimeSearcher() {
//        try {            
//            IndexReader r = indexWriter.getReader();
//            return new IndexSearcher(r);
//                    
//        } catch (IOException e){
//            String msg = "Fatal error getting Lucene IndexReader for "+indexWriter.getDirectory();
//            throw new PersistenceLuceneException(msg, e);
//        }
//    }
    
    private IndexSearcher createDirectorySearcher() {
        try {
            Directory directory = indexWriter.getDirectory();
            IndexReader r = IndexReader.open(directory);
            return new IndexSearcher(r);
        } catch (IOException e){
            throw new PersistenceLuceneException(e);
        }
    }
}
