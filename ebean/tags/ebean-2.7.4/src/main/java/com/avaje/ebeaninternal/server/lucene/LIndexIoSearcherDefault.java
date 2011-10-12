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

public class LIndexIoSearcherDefault implements LIndexIoSearcher {

    private static final Logger logger = Logger.getLogger(LIndexIoSearcherDefault.class.getName());
    
    private final String name;
    
    private final IndexWriter indexWriter;
    
    private volatile LIndexSearch indexSearch;
    
    public LIndexIoSearcherDefault(IndexWriter indexWriter, String name) {
        this.name = name;
        this.indexWriter = indexWriter;
        this.indexSearch = refreshIndexSearch();
    }

    public void postCommit() {
        try {
            refreshIndexSearch();
        } catch (Exception e){
            String msg = "Error postCommit() refreshing IndexSearcher";
            logger.log(Level.SEVERE, msg, e);
        }
    }
    
    
    public void refresh(boolean nearRealTime) {
        try {
            refreshIndexSearch();
        } catch (Exception e){
            String msg = "Error refreshing IndexSearch";
            logger.log(Level.SEVERE, msg, e);
        }
    }
    
    public LIndexVersion getLastestVersion() {
        
        LIndexSearch s = this.indexSearch;
        try {
            IndexCommit c = s.getIndexReader().getIndexCommit();
            return new LIndexVersion(c.getGeneration(), c.getVersion());
            
        } catch (IOException e) {
            throw new PersistenceLuceneException(e);
            
        }
//        finally {
//            try {
//                s.getIndexReader().decRef();
//            } catch (IOException e) {
//                logger.log(Level.WARNING, "Error on IndexReader.decRef()", e);
//            }
//        }
    }
    
    public LIndexSearch getIndexSearch() {
        
        LIndexSearch s = this.indexSearch;
        
        if (s.isOpenAcquire()) {
            return s;
        } else {
            // current one has been marked for close
            return refreshIndexSearch();
        }
    }
    
    private LIndexSearch refreshIndexSearch() {
        
        synchronized (this) {
        
            try {
                LIndexSearch currentSearch = this.indexSearch;
                
                IndexReader newReader;
                if (currentSearch == null) {
                    // only on creation
                    newReader = createIndexReader();
                    
                } else {
                    newReader = currentSearch.getIndexReader().reopen();
                
                    if (newReader == currentSearch.getIndexReader()) {
                        // no changes for reader
                        return currentSearch;    
                    } 
                }
                
                // reader was reopened
                IndexSearcher searcher = new IndexSearcher(newReader);
                //searcher.setSimilarity(similarity);
                //searcher.setDefaultFieldSortScoring(doTrackScores, doMaxScore);
                
                LIndexSearch newSearch = new LIndexSearch(searcher, newReader);
                
                if (currentSearch != null) {
                    currentSearch.markForClose();
                }
                logger.info("Lucene Searcher refreshed "+name);
                this.indexSearch = newSearch;
                return newSearch;
                
            } catch (IOException e){
                throw new PersistenceLuceneException(e);
            }
        }
    }
    
    private IndexReader createIndexReader() {
        try {
            Directory directory = indexWriter.getDirectory();
            return IndexReader.open(directory);
            
        } catch (IOException e){
            throw new PersistenceLuceneException(e);
        }
    }
}
