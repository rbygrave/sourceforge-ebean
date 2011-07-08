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
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;

import com.avaje.ebean.config.lucene.LuceneIndex;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryDetail;

public class LIndex implements LuceneIndex {

    //private static final Logger logger = Logger.getLogger(LIndex.class.getName());
    
    private final DefaultLuceneIndexManager manager;
    
    private final String name;
    
    private final Analyzer analyzer;
    
    private final LIndexFields fieldDefn;
    
    private final BeanDescriptor<?> desc;

    private final OrmQueryDetail ormQueryDetail;
    
    private final LIndexIo indexIo;

    private final LIndexFieldId idField;
    
    private final Object syncMonitor = new Object();
    
    private boolean runningSync;
    
    private LIndexSync queuedSync;
    
    public LIndex(DefaultLuceneIndexManager manager, String indexName, String indexDir, Analyzer analyzer, 
            BeanDescriptor<?> desc, LIndexFields fieldDefn, String[] updateProps) throws IOException {
        
        this.manager = manager;
        this.name = desc.getFullName();
        this.analyzer = analyzer;
        this.desc = desc;
        this.fieldDefn = fieldDefn;
        this.idField = fieldDefn.getIdField();
        this.ormQueryDetail = fieldDefn.getOrmQueryDetail();
        
        this.indexIo = new LIndexIo(manager, indexDir, this, updateProps); 
        manager.addIndex(this);
        fieldDefn.registerIndexWithProperties(this);
    }
    
    /**
     * A sync was finished.
     */
    protected void syncFinished(boolean success) {
        
        // TODO If error could try 5 times to sync ?
        synchronized (syncMonitor) {
            runningSync = false;
        }
    }
    
    /**
     * Queue a sync to execute in a background thread.
     * <p>
     * If there is already a sync running then queue it to run again when the
     * currently running sync has finished.
     * </p>
     */
    public void queueSync(String masterHost) {
        
        synchronized (syncMonitor) {
            LIndexSync sync  = new LIndexSync(this, masterHost);
            if (!runningSync){
                // run this is the background
                runningSync = true;
                manager.execute(sync);
            } else {
                // a sync is already in process so just queue it
                // to run again after it has finished. Note its
                // okay to overwrite a previous queuedSync as we
                // just want to run the last one really
                queuedSync = sync;
            }
        }
    }

    /**
     * Called periodically to commit or run queued sync.
     */
    public void manage(LuceneIndexManager indexManager) {
        
        synchronized (syncMonitor) {
            indexIo.manage(indexManager);
            if (!runningSync && queuedSync != null){
                // run a queuedSync
                LIndexSync sync = queuedSync;
                runningSync = true;
                queuedSync = null;
                manager.execute(sync);
            }
        }
    }
    
    public void addNotifyCommitRunnable(Runnable r) {
        indexIo.addNotifyCommitRunnable(r);
    }
    
    public LIndexVersion getLastestVersion() {
        return indexIo.getLastestVersion();
    }
    
    public File getIndexDir() {
        return indexIo.getIndexDir();
    }
    
    public void refresh(boolean nearRealTime) {
        indexIo.refresh(nearRealTime);
    }
    
    public LIndexFileInfo getLocalFile(String fileName) {
        return indexIo.getLocalFile(fileName);
    }
    
    public LIndexCommitInfo obtainLastIndexCommitIfNewer(long remoteIndexVersion) {
        return indexIo.obtainLastIndexCommitIfNewer(remoteIndexVersion);
    }
    
    public void releaseIndexCommit(long remoteIndexVersion) {
        indexIo.releaseIndexCommit(remoteIndexVersion);
    }
    
    public LIndexFileInfo getFile(long remoteIndexVersion, String fileName) {
        return indexIo.getFile(remoteIndexVersion, fileName);
    }
    
    public Term createIdTerm(Object id) {
        return idField.createTerm(id);
    }
    
    public void shutdown() {
        indexIo.shutdown();
    }
    
    public LIndexUpdateFuture rebuild() {
        LIndexUpdateFuture future = new LIndexUpdateFuture(desc.getBeanType());
        indexIo.addWorkToQueue(LIndexWork.newRebuild(future));
        return future;
    }
    
    public LIndexUpdateFuture update() {
        LIndexUpdateFuture future = new LIndexUpdateFuture(desc.getBeanType());
        indexIo.addWorkToQueue(LIndexWork.newQueryUpdate(future));
        return future;
    }

    public String toString() {
        return name;
    }
    
    public String getName() {
        return name;
    }

    public Class<?> getBeanType() {
        return desc.getBeanType();
    }

    public BeanDescriptor<?> getBeanDescriptor() {
        return desc;
    }

    public LIndexSearch getIndexSearch() {
        return indexIo.getIndexSearch();
    }
    
    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public QueryParser createQueryParser(String fieldName) {
        QueryParser p =  fieldDefn.createQueryParser(fieldName);
        p.setDefaultOperator(Operator.AND);
        return p;
    }
    
    public LIndexFields getIndexFieldDefn() {
        return fieldDefn;
    }
    
    public Set<String> getResolvePropertyNames() {
        return fieldDefn.getResolvePropertyNames();
    }

    public OrmQueryDetail getOrmQueryDetail() {
        return ormQueryDetail;
    }

    public Object readDocument(Document doc){
        
        Object bean = desc.createEntityBean();
        fieldDefn.readDocument(doc, bean);
        return bean;
    }

    public DocFieldWriter createDocFieldWriter() {
        return fieldDefn.createDocFieldWriter();
    }

    public SpiQuery<?> createQuery() {
        return indexIo.createQuery();
    }
    
    public LIndexUpdateFuture process(IndexUpdates indexUpdates) {
        
        List<TableIUD> tableList = indexUpdates.getTableList();
        if (tableList != null && tableList.size() > 0){
            boolean bulkDelete = false;
            for (int i = 0; i < tableList.size(); i++) {
                TableIUD bulkTableEvent = tableList.get(i);
                if (bulkTableEvent.isDelete()){
                    bulkDelete = true;
                }
            }
            if (bulkDelete){
                return rebuild();
            } else {
                return update();
            }
        }
        
        if (indexUpdates.isInvalidate()){
            return update();
        }

        LIndexUpdateFuture f = new LIndexUpdateFuture(desc.getBeanType());
        indexIo.addWorkToQueue(LIndexWork.newTxnUpdate(f, indexUpdates));
        return f;
    }

}
