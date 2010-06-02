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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.avaje.ebean.QueryListener;
import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryDetail;
import com.avaje.ebeaninternal.server.transaction.BeanDelta;

public class LIndexIo {

    private static final Logger logger = Logger.getLogger(LIndexIo.class.getName());
    
    private final LuceneIndexManager manager;
    
    private final String indexDir;
    
    private final LIndex index;
    
    private final Analyzer analyzer;
    
    private final MaxFieldLength maxFieldLength;
    
    private final Class<?> beanType;
    
    private final OrmQueryDetail ormQueryDetail;
    
    private final Directory directory;
    
    private final BeanDescriptor<?> beanDescriptor;
    
    private final IndexWriter indexWriter;
    
    private final LIndexIoSearcher ioSearcher;
    
    private long queueCommitStart;
    
    private int queueCommitCount;
    
    private int totalCommitCount;
    private long totalCommitNanos;
    private long totalPostCommitNanos;

    public LIndexIo(LuceneIndexManager manager, String indexDir, LIndex index) throws IOException {
        this.manager = manager;
        this.indexDir = indexDir;
        this.index = index;
        this.analyzer = index.getAnalyzer();
        this.maxFieldLength = index.getMaxFieldLength();
        this.beanType = index.getBeanType();
        this.ormQueryDetail = index.getOrmQueryDetail();
        this.directory = getDirectory();
        this.beanDescriptor = index.getBeanDescriptor();
        
        this.indexWriter = createIndexWriter();
        this.ioSearcher = createIoSearcher();
    }
    
    public void manage(LuceneIndexManager indexManager) {
        if (commit()){
            
        }
    }
    
    private LIndexIoSearcher createIoSearcher() {
        
        // FIXME: Also have commit based implementation 
        return new LIndexIoSearcherNearRealTime(indexWriter);
    }
    

    public void shutdown() {
        synchronized (indexWriter) { 
            try {
                if (queueCommitStart > 0){
                    indexWriter.commit();
                }
            } catch (Exception e) {
                String msg = "Error committing queued changes for IndexWriter for "+indexDir;
                logger.log(Level.SEVERE, msg, e);
                // Also send to syserr during shutdown
                e.printStackTrace();
            } finally {
                try {
                    indexWriter.close();                
                } catch (Exception e) {
                    String msg = "Error closing IndexWriter for "+indexDir;
                    logger.log(Level.SEVERE, msg, e);
                    // Also send to syserr during shutdown
                    e.printStackTrace();
                }
            }
        }
    }
    
    public LIndexDeltaHandler createDeltaHandler(List<BeanDelta> deltaBeans) {
        
        IndexSearcher searcher = getIndexSearcher();
        IndexWriter indexWriter = getIndexWriter();
        DocFieldWriter docFieldWriter = index.createDocFieldWriter();
        return new LIndexDeltaHandler(index, searcher, indexWriter, analyzer, beanDescriptor, docFieldWriter, deltaBeans);
    }

    public IndexWriter getIndexWriter() {
        return indexWriter;
    }
    
    public IndexSearcher getIndexSearcher() {
        return ioSearcher.getIndexSearcher();
    }
    
    public void commitQueuedChanges(long freqMillis) {
        synchronized (indexWriter) {
            if (queueCommitStart > 0){
                if (freqMillis == 0 || (System.currentTimeMillis() - freqMillis) > queueCommitStart ){
                    commit();
                }
            }
        }
    }
    
    /**
     * Queue a commit for execution later via the Lucene Manager thread.
     */
    public void queueCommit() {
        synchronized (indexWriter) {
            if (queueCommitStart == 0){
                queueCommitStart = System.currentTimeMillis();
            }
            queueCommitCount++;
        }
    }
    
    public long getQueueCommitStart(boolean reset) {
        synchronized (indexWriter) {   
            long start = this.queueCommitStart;
            if (reset){
                this.queueCommitStart = 0;
                this.queueCommitCount = 0;
            }
            return start;
        }
    }
    
    public boolean commit() {
        synchronized (indexWriter) { 
            try {
                if (queueCommitStart == 0){
                    return false;
                }
                if (logger.isLoggable(Level.INFO)){
                    String delayMsg;
                    if (queueCommitStart > 0){
                        long delay = System.currentTimeMillis()-queueCommitStart;
                        delayMsg = " queueDelayMillis:"+delay+" queueCount:"+queueCommitCount;
                    } else {
                        delayMsg = "";
                    }
                    String m = "Lucene commit "+indexDir+delayMsg;
                    logger.info(m);
                }
                long nanoStart = System.nanoTime();
                indexWriter.commit();
                queueCommitStart = 0;
                queueCommitCount = 0;

                long nanoCommit = System.nanoTime();
                long nanoCommitExe = nanoCommit - nanoStart;

                ioSearcher.postCommit();
                long nanoPostCommitExe = System.nanoTime() - nanoCommitExe;
                
                totalCommitCount++;
                totalCommitNanos += nanoCommitExe;
                totalPostCommitNanos += nanoPostCommitExe;
                
                return true;
                
            } catch (IOException e) {
                String msg = "Error committing changes on index "+indexDir;
                throw new PersistenceLuceneException(msg, e);
            }
        }
    }
        
    public SpiQuery<?> createQuery() {
        
        SpiEbeanServer server = manager.getServer();
        SpiQuery<?> query = (SpiQuery<?>)server.createQuery(beanType);
        query.setUseIndex(UseIndex.NO);
        query.getDetail().tuneFetchProperties(ormQueryDetail);
        
        return query;
    }
    
    private Directory getDirectory() throws IOException {
        File dir = new File(indexDir);
        return FSDirectory.open(dir); 
    }
    
    private IndexWriter createIndexWriter() {
        try {
            boolean create = true;
            return new IndexWriter(directory, analyzer, create, maxFieldLength);
        } catch (IOException e) {
            String msg = "Error getting Lucene IndexWriter for " + indexDir;
            throw new PersistenceLuceneException(msg, e);
        }
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
            indexWriter.commit();
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
