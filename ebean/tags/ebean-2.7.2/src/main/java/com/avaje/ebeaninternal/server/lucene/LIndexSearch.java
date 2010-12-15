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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

public class LIndexSearch {

    private static final Logger logger = Logger.getLogger(LIndexSearch.class.getName());
    
    private final IndexSearcher indexSearcher;

    private final IndexReader indexReader;

    private int refCount;

    private boolean markForClose;

    private boolean closed;
    
    public LIndexSearch(IndexSearcher indexSearcher, IndexReader indexReader) {
        this.indexSearcher = indexSearcher;
        this.indexReader = indexReader;
    }
    
    public IndexSearcher getIndexSearcher() {
        return indexSearcher;
    }

    public IndexReader getIndexReader() {
        return indexReader;
    }

    public boolean isOpenAcquire() {
        synchronized(this) {
            if (markForClose) {
                return false;
            }
            refCount++;
            return true;
        }
    }

    public void releaseClose() {
        synchronized(this) {
            refCount--;
            closeIfMarked();
        }
    }

    public void markForClose() {
        synchronized(this) {
            markForClose = true;
            closeIfMarked();
        }
    }

    private void closeIfMarked() {
        
        if (markForClose && refCount <= 0 && !closed) {
            
            closed = true;
            try {
                indexSearcher.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error when closing indexSearcher", e);
            }
            try {
                indexReader.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error when closing indexReader", e);
            }
        }
    }
}
