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

import javax.persistence.PersistenceException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.avaje.ebean.QueryListener;
import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryDetail;

public class LIndexIO {

    private final LuceneIndexManager manager;
    
    private final String indexDir;
    
    private final LIndex index;
    
    private final Analyzer analyzer;
    
    private final MaxFieldLength maxFieldLength;
    
    private final Class<?> beanType;
    
    private final OrmQueryDetail ormQueryDetail;
    
    private final Directory directory;
    
    public LIndexIO(LuceneIndexManager manager, String indexDir, LIndex index) throws IOException {
        this.manager = manager;
        this.indexDir = indexDir;
        this.index = index;
        this.analyzer = index.getAnalyzer();
        this.maxFieldLength = index.getMaxFieldLength();
        this.beanType = index.getBeanType();
        this.ormQueryDetail = index.getOrmQueryDetail();
        this.directory = getDirectory();

    }
    
    public IndexWriter getIndexWriter() {
        try {
            // Not an efficient mechanism yet
            boolean create = true;
            return new IndexWriter(directory, analyzer, create, maxFieldLength);

        } catch (IOException e) {
            String msg = "Error getting Lucene IndexWriter for " + indexDir;
            throw new PersistenceException(msg, e);
        }
    }
    
    public Searcher getSearcher() {
        try {
            // Not an efficient mechanism yet
            boolean readOnly = true;
            IndexReader reader = IndexReader.open(directory, readOnly);
        
            return new IndexSearcher(reader);
            
        } catch (IOException e){
            String msg = "Error getting Lucene IndexReader for "+indexDir;
            throw new PersistenceException(msg, e);
        }
    }
    
    private Directory getDirectory() throws IOException {
        File dir = new File(indexDir);
        return FSDirectory.open(dir); 
    }
    
    public SpiQuery<?> createQuery() {
        
        SpiEbeanServer server = manager.getServer();
        SpiQuery<?> query = (SpiQuery<?>)server.createQuery(beanType);
        query.setUseIndex(UseIndex.NO);
        query.getDetail().tuneFetchProperties(ormQueryDetail);
        
        return query;
    }
    
    @SuppressWarnings("unchecked")
    public int rebuild() throws IOException {
        
        IndexWriter indexWriter = getIndexWriter();
        try {
            indexWriter.deleteAll();
            
            SpiQuery<?> query = createQuery();
            
            WriteListener writeListener = new WriteListener(index, indexWriter);
            query.setListener(writeListener);
            
            manager.getServer().findList(query, null);
                   
            return writeListener.getCount();
            
        } finally {
            indexWriter.getReader();
            indexWriter.close();
        }
    }
    
    @SuppressWarnings("unchecked")
    private static class WriteListener implements QueryListener {

        private final IndexWriter indexWriter;
        private final DocFieldWriter docFieldWriter;
        private final Document document = new Document();
        private int count;
        
        private WriteListener(LIndex index,IndexWriter indexWriter) {
            this.indexWriter = indexWriter;
            this.docFieldWriter = index.createDocFieldWriter();
        }
        
        public void process(Object bean) {
            try {
                docFieldWriter.writeValue(bean, document);
                indexWriter.addDocument(document);
                count++;
            } catch (Exception e) {
                throw new PersistenceException(e);
            }
        }

        public int getCount() {
            return count;
        }
        
    }
    
}
