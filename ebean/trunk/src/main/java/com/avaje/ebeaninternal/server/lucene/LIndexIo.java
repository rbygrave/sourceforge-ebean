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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.avaje.ebean.Junction;
import com.avaje.ebean.Query.UseIndex;
import com.avaje.ebean.QueryListener;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.lucene.LIndexWork.WorkType;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryDetail;
import com.avaje.ebeaninternal.server.transaction.IndexEvent;

public class LIndexIo {

    private static final Logger logger = Logger.getLogger(LIndexIo.class.getName());
    
    private final LuceneIndexManager manager;
    
    private final String indexDir;
    
    private final LIndex index;
    
    private final Analyzer analyzer;
    
    //private final MaxFieldLength maxFieldLength;
    
    private final Class<?> beanType;
    
    private final OrmQueryDetail ormQueryDetail;
    
    private final Directory directory;
    
    private final BeanDescriptor<?> beanDescriptor;
    
    private final IndexWriter indexWriter;
    
    private final LIndexIoSearcher ioSearcher;
    
    private final HoldAwareIndexDeletionPolicy commitDeletionPolicy;
    
    private final String[] updateProps;
    
    private final Object writeMonitor = new Object();
    
    private final Object workQueueMonitor = new Object();
    
    private final ArrayList<LIndexWork> workQueue = new ArrayList<LIndexWork>();
    
    private final ArrayList<Runnable> notifyCommitRunnables = new ArrayList<Runnable>();
    
    private long lastUpdateTime;

    private long queueCommitStart;
    
    private int queueCommitCount;
    
    private int totalCommitCount;
    
    private long totalCommitNanos;
    
    private long totalPostCommitNanos;

    public LIndexIo(LuceneIndexManager manager, String indexDir, LIndex index, String[] updateProps) throws IOException {
        this.manager = manager;
        this.indexDir = indexDir;
        this.index = index;
        this.updateProps = updateProps;
        this.analyzer = index.getAnalyzer();
        //this.maxFieldLength = index.getMaxFieldLength();
        this.beanType = index.getBeanType();
        this.ormQueryDetail = index.getOrmQueryDetail();
        this.directory = createDirectory();
        this.beanDescriptor = index.getBeanDescriptor();
        
        this.commitDeletionPolicy = new HoldAwareIndexDeletionPolicy(indexDir);
        this.indexWriter = createIndexWriter();
        
        this.ioSearcher = createIoSearcher();
    }
    
    public LIndexVersion getLastestVersion(){
        return ioSearcher.getLastestVersion();
    }

    public long getLastVersion() {
        return commitDeletionPolicy.getLastVersion();
    }
    
    public LIndexCommitInfo obtainLastIndexCommitIfNewer(long remoteIndexVersion) {
        return commitDeletionPolicy.obtainLastIndexCommitIfNewer(remoteIndexVersion);
    }
    
    public File getIndexDir() {
        return new File(indexDir);
    }
    
    public LIndexFileInfo getLocalFile(String fileName) {

        File f = new File(indexDir, fileName);
        return new LIndexFileInfo(f);
    }
    
    public void refresh(boolean nearRealTime) {
        ioSearcher.refresh(nearRealTime);
    }
    
    
    public LIndexFileInfo getFile(long remoteIndexVersion, String fileName) {
        
        commitDeletionPolicy.touch(remoteIndexVersion);
        
        File f = new File(indexDir, fileName);
        return new LIndexFileInfo(f);
    }
    
    public void releaseIndexCommit(long remoteIndexVersion) {
        commitDeletionPolicy.releaseIndexCommit(remoteIndexVersion);
    }
    
    public void shutdown() {
        synchronized (writeMonitor) { 
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
    
    protected void manage(LuceneIndexManager indexManager) {
        processWorkQueue();
        commit(false);
    }
    
    protected void addWorkToQueue(LIndexWork work){
        synchronized (workQueueMonitor) {
            workQueue.add(work);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void processWorkQueue() {
        synchronized (workQueueMonitor) {
            if (!workQueue.isEmpty()){
                
                WorkType maxWorkType = null;
                
                for (int i = 0; i < workQueue.size(); i++) {
                    LIndexWork work = workQueue.get(i);
                    if (maxWorkType == null || maxWorkType.ordinal() < work.getWorkType().ordinal()){
                        maxWorkType = work.getWorkType();
                    }
                }
                List<LIndexWork> workQueueClone = (List<LIndexWork>)workQueue.clone();
                workQueue.clear();
                
                Callable<Integer> workCallable = getWorkCallable(maxWorkType, workQueueClone);
                FutureTask<Integer> ft = new FutureTask<Integer>(workCallable);
                
                for (int i = 0; i < workQueueClone.size(); i++) {
                    workQueueClone.get(i).getFuture().setTask(ft);
                }
                
                manager.getServer().getBackgroundExecutor().execute(ft);
                
            }
        }
    }
    
    private Callable<Integer> getWorkCallable(WorkType maxWorkType, List<LIndexWork> workQueueClone) {
        switch (maxWorkType) {
        case REBUILD:
            return newRebuildCallable(workQueueClone);
        case QUERY_UPDATE:
            return newQueryUpdateCallable(workQueueClone);
        case TXN_UPDATE:
            return newTxnUpdateCallable(workQueueClone);

        default:
            throw new IllegalStateException("Unknown workType "+maxWorkType);
        }
    }
    
    private Callable<Integer> newTxnUpdateCallable(List<LIndexWork> workQueueClone) {
        
        final List<LIndexWork> updates = workQueueClone;
        
        return new Callable<Integer>() {
            public String toString() {
                return "TxnUpdate";
            }
            
            public Integer call() throws IOException {
                
                int totalDocs = 0;
                
                for (int i = 0; i < updates.size(); i++) {
                    LIndexWork lIndexWork = updates.get(i);
                    IndexUpdates indexUpdates = lIndexWork.getIndexUpdates();
                    LIndexDeltaHandler h = createDeltaHandler(indexUpdates);
                    totalDocs += h.process();
                }
                
                queueCommit(updates);
                
                return totalDocs;
            }
        };
    }
    
    private Callable<Integer> newRebuildCallable(List<LIndexWork> workQueueClone) {
        return new QueryUpdater(true, workQueueClone);
    }
    
    private Callable<Integer> newQueryUpdateCallable(List<LIndexWork> workQueueClone) {
        return new QueryUpdater(false, workQueueClone);
    }
    
    class QueryUpdater implements Callable<Integer> {
        
        private final boolean rebuild;
        private final List<LIndexWork> workQueueClone;
        
        private QueryUpdater(boolean rebuild, List<LIndexWork> workQueueClone){
            this.rebuild = rebuild;
            this.workQueueClone = workQueueClone;
        }
        
        public String toString() {
            return rebuild ? "Rebuild" : "QueryUpdate";
        }
        
        public Integer call() throws Exception {
            if (rebuild){
                return rebuildIndex(workQueueClone);
            } else {
                return updateIndex(workQueueClone);
            }
        }
    }

    private LIndexDeltaHandler createDeltaHandler(IndexUpdates indexUpdates) {
        
        LIndexSearch search = getIndexSearch();
        IndexWriter indexWriter = this.indexWriter;
        DocFieldWriter docFieldWriter = index.createDocFieldWriter();
        return new LIndexDeltaHandler(index, search, indexWriter, analyzer, beanDescriptor, docFieldWriter, indexUpdates);
    }
    
    public LIndexSearch getIndexSearch() {
        return ioSearcher.getIndexSearch();
    }
    
//    public void commitQueuedChanges(long freqMillis) {
//        synchronized (writeMonitor) {
//            if (queueCommitStart > 0){
//                if (freqMillis == 0 || (System.currentTimeMillis() - freqMillis) > queueCommitStart ){
//                    commit(false);
//                }
//            }
//        }
//    }

    /**
     * Queue a commit for execution later via the Lucene Manager thread.
     */
    private void queueCommit(List<LIndexWork> workQueueClone) {
        synchronized (workQueueMonitor) {
            if (queueCommitStart == 0){
                queueCommitStart = System.currentTimeMillis();
            }
            queueCommitCount++;
            
            for (LIndexWork w : workQueueClone) {
                // register so that on the next commit these are run
                notifyCommitRunnables.add(w.getFuture().getCommitRunnable());
            }
        }
    }
    
    protected void addNotifyCommitRunnable(Runnable r) {
        synchronized (workQueueMonitor) {   
            notifyCommitRunnables.add(r);
        }
    }
    
    protected long getQueueCommitStart(boolean reset) {
        synchronized (workQueueMonitor) {   
            long start = this.queueCommitStart;
            if (reset){
                this.queueCommitStart = 0;
                this.queueCommitCount = 0;
            }
            return start;
        }
    }
    
    /**
     * Invoke a commit if there are uncommitted changes. Return true if commit
     * occurred or false if no commit was required.
     */
    private boolean commit(boolean force) {
        
        synchronized (writeMonitor) { 
            
            long start = 0;
            long count = 0;
            ArrayList<Runnable> notifyRunnables = new ArrayList<Runnable>();
            
            synchronized (workQueueMonitor) {
                start = queueCommitStart;
                count = queueCommitCount;
                queueCommitStart = 0;
                queueCommitCount = 0;
                notifyRunnables.addAll(notifyCommitRunnables);
                notifyCommitRunnables.clear();
            }
            
            try {
                if (!force && start == 0){
                    // no pending uncommitted changes
                    // so just return false
                    
                    if (!notifyRunnables.isEmpty()){
                        // Add notifyRunnables back onto the notify queue
                        for (int i = 0; i < notifyRunnables.size(); i++) {
                            addNotifyCommitRunnable(notifyRunnables.get(i));
                        }                        
                    }
                    return false;
                }
                if (logger.isLoggable(Level.INFO)){
                    String delayMsg;
                    if (queueCommitStart > 0){
                        long delay = System.currentTimeMillis()-start;
                        delayMsg = " queueDelayMillis:"+delay+" queueCount:"+count;
                    } else {
                        delayMsg = "";
                    }
                    String m = "Lucene commit "+indexDir+delayMsg;
                    logger.info(m);
                }
                long nanoStart = System.nanoTime();
                
                // do the actual commit
                indexWriter.commit();


                long nanoCommit = System.nanoTime();
                long nanoCommitExe = nanoCommit - nanoStart;

                // notify the searcher
                ioSearcher.postCommit();
                
                for (int i = 0; i < notifyRunnables.size(); i++) {
                    notifyRunnables.get(i).run();
                }
                
                long nanoPostCommitExe = System.nanoTime() - nanoCommitExe;
                
                totalCommitCount++;
                totalCommitNanos += nanoCommitExe;
                totalPostCommitNanos += nanoPostCommitExe;
                
                IndexEvent indexEvent = new IndexEvent(IndexEvent.COMMIT_EVENT, index.getName());
                manager.notifyCluster(indexEvent);
                
                return true;
                
            } catch (IOException e) {
                String msg = "Error committing changes on index "+indexDir;
                throw new PersistenceLuceneException(msg, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private int rebuildIndex(List<LIndexWork> workQueueClone) throws IOException {
        synchronized (writeMonitor) { 
            
            logger.info("Lucene rebuild "+indexDir);
            
            //TODO: Parallel index rebuild
            try {
                indexWriter.deleteAll();
                lastUpdateTime = System.currentTimeMillis();
                SpiQuery<?> query = createQuery();
                
                WriteListener writeListener = new WriteListener(index, indexWriter, false);
                query.setListener(writeListener);
                
                manager.getServer().findList(query, null);
                 
                return writeListener.getCount();
                
            } finally {
                queueCommit(workQueueClone);
                commit(false);
            }
        }
    }
        
    @SuppressWarnings("unchecked")
    private int updateIndex(List<LIndexWork> workQueueClone) throws IOException {
        synchronized (writeMonitor) { 
            
            logger.info("Lucene update "+indexDir);
            
            try {
                long updateTime = System.currentTimeMillis();
                SpiQuery<?> query = createUpdateQuery();
                lastUpdateTime = updateTime;
                
                WriteListener writeListener = new WriteListener(index, indexWriter, true);
                query.setListener(writeListener);
                
                manager.getServer().findList(query, null);
                 
                return writeListener.getCount();
                
            } finally {
                queueCommit(workQueueClone);
            }
        }
    }
    
    private SpiQuery<?> createUpdateQuery() {
        
        SpiQuery<?> q = createQuery();
        Junction<?> disjunction = q.where().disjunction();
        
        Timestamp lastUpdate = new Timestamp(lastUpdateTime);
        
        for (int i = 0; i < updateProps.length; i++) {
            disjunction.ge(updateProps[i], lastUpdate);
        }
        
        return q;
    }
    
    protected SpiQuery<?> createQuery() {
        
        SpiEbeanServer server = manager.getServer();
        SpiQuery<?> query = (SpiQuery<?>)server.createQuery(beanType);
        query.setUseIndex(UseIndex.NO);
        query.getDetail().tuneFetchProperties(ormQueryDetail);
        
        return query;
    }
    
    private Directory createDirectory() throws IOException {
        File dir = new File(indexDir);
        return FSDirectory.open(dir); 
    }
    
    private IndexWriter createIndexWriter() {
        try {
            
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_33, analyzer);
            config.setOpenMode(OpenMode.CREATE_OR_APPEND);
            config.setIndexDeletionPolicy(commitDeletionPolicy);
            
            IndexWriter w = new IndexWriter(directory, config);
            w.commit();
                        
            return w;
            
        } catch (IOException e) {
            String msg = "Error getting Lucene IndexWriter for " + indexDir;
            throw new PersistenceLuceneException(msg, e);
        }
    }
    
    private LIndexIoSearcher createIoSearcher() {
        
        return new LIndexIoSearcherDefault(indexWriter, index.getName());
    }
  
    @SuppressWarnings("rawtypes")
    private static class WriteListener implements QueryListener {

        private final boolean updateMode;
        private final LIndex index;
        private final BeanDescriptor beanDescriptor;
        private final IndexWriter indexWriter;
        private final DocFieldWriter docFieldWriter;
        private final Document document = new Document();
        private int count;
        
        private WriteListener(LIndex index,IndexWriter indexWriter, boolean updateMode) {
            this.updateMode = updateMode;
            this.index = index;
            this.beanDescriptor = index.getBeanDescriptor();
            this.indexWriter = indexWriter;
            this.docFieldWriter = index.createDocFieldWriter();
        }
        
        public void process(Object bean) {
            try {
                if (updateMode) {
                    Object id = beanDescriptor.getId(bean);
                    Term term = index.createIdTerm(id);
                    indexWriter.deleteDocuments(term);
                }
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
