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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;

import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.transaction.BeanDelta;
import com.avaje.ebeaninternal.server.transaction.BeanDeltaList;
import com.avaje.ebeaninternal.server.transaction.BeanPersistIds;

public class LIndexDeltaHandler {

    private static final Logger logger = Logger.getLogger(LIndexDeltaHandler.class.getName());
    
    private final LIndex index;
    
    private final LIndexSearch search;

    private final IndexSearcher searcher;

    private final IndexWriter indexWriter;

    private final Analyzer analyzer;

    private final BeanDescriptor<?> beanDescriptor;

    private final DocFieldWriter docFieldWriter;

    private final IndexUpdates indexUpdates;
    
    private final List<BeanDelta> deltaBeans;

    private Set<Object> deltaBeanKeys;
    private int deltaCount;
    private int insertCount;
    private int updateCount;
    private int deleteCount;
    private int deleteByIdCount;
    
    
    public LIndexDeltaHandler(LIndex index, LIndexSearch search, IndexWriter indexWriter, Analyzer analyzer,
            BeanDescriptor<?> beanDescriptor, DocFieldWriter docFieldWriter, IndexUpdates indexUpdates) {

        this.index = index;
        this.search = search;
        this.searcher = search.getIndexSearcher();
        this.indexWriter = indexWriter;
        this.analyzer = analyzer;
        this.beanDescriptor = beanDescriptor;
        this.docFieldWriter = docFieldWriter;
        this.indexUpdates = indexUpdates;
        
        BeanDeltaList deltaList = indexUpdates.getDeltaList();
        this.deltaBeans = deltaList == null ? null : deltaList.getDeltaBeans();
    }

    public int process() {
        deltaBeanKeys = processDeltaBeans();
        deltaCount = deltaBeanKeys.size();
        
        BeanPersistIds deleteById = indexUpdates.getDeleteIds();
        if (deleteById != null ){
            deleteByIdCount = processDeletes(deleteById.getDeleteIds());
        }
        
        BeanPersistIds beanPersistIds = indexUpdates.getBeanPersistIds();
        if (beanPersistIds != null){
            deleteCount = processDeletes(beanPersistIds.getDeleteIds());
            processInserts(beanPersistIds.getInsertIds());
            processUpdates(beanPersistIds.getUpdateIds());
        }
        
        String msg = String.format("Lucene update index %s deltas[%s] insert[%s] update[%s] delete[%s]",
                index, deltaCount, insertCount, updateCount, (deleteCount+deleteByIdCount));
        
        logger.info(msg);
        
        return deltaCount + insertCount + updateCount + deleteCount + deleteByIdCount;
    }

    private void processUpdates(List<Serializable> updateIds) {

        if (updateIds == null || updateIds.isEmpty()){
            return;
        }
        
        ArrayList<Object> filterIdList = new ArrayList<Object>();
        
        // filter out Id's that where already 
        // processed as a BeanDelta
        for (int i = 0; i < updateIds.size(); i++) {
            Serializable id = updateIds.get(i);
            if (!deltaBeanKeys.contains(id)){
                filterIdList.add(id);
            }
        }
        
        if (!filterIdList.isEmpty()) {
            SpiQuery<?> ormQuery = index.createQuery();
            ormQuery.where().idIn(filterIdList);
            
            List<?> list = ormQuery.findList();
            for (int i = 0; i < list.size(); i++) {
                Object bean = list.get(i);
                try {
                    Object id = beanDescriptor.getId(bean);
                    Term term = index.createIdTerm(id);
                    Document document = new Document();
                    docFieldWriter.writeValue(bean, document);
                    indexWriter.updateDocument(term, document);
                    
                } catch (Exception e) {
                    throw new PersistenceException(e);
                }
            }
            updateCount = list.size();
        }
    }
    
    private void processInserts(List<Serializable> insertIds) {
    
        if (insertIds == null || insertIds.isEmpty()) {
            return;
        }
        SpiQuery<?> ormQuery = index.createQuery();
        ormQuery.where().idIn(insertIds);
        List<?> list = ormQuery.findList();
        for (int i = 0; i < list.size(); i++) {
            Object bean = list.get(i);
            try {
            	Document document = new Document();
                docFieldWriter.writeValue(bean, document);
                indexWriter.addDocument(document);
                
            } catch (Exception e) {
                throw new PersistenceException(e);
            }
        }
        insertCount = list.size();
    }
    
    private int processDeletes(List<Serializable> deleteIds) {
    
        if (deleteIds == null || deleteIds.isEmpty()){
            return 0;
        }
        for (int i = 0; i < deleteIds.size(); i++) {
            Serializable id = deleteIds.get(i);
            Term term = index.createIdTerm(id);
            try {
                indexWriter.deleteDocuments(term);
            } catch (Exception e) {
                throw new PersistenceLuceneException(e);
            }
        }
        return deleteIds.size();
    }
    
    private Set<Object> processDeltaBeans() {

        if (deltaBeans == null){
            return Collections.emptySet();
        }
        try {
            LinkedHashMap<Object, Object> beanMap = getBeans();
            
            for (int i = 0; i < deltaBeans.size(); i++) {
                BeanDelta deltaBean = deltaBeans.get(i);
                Object id = deltaBean.getId();
                Object bean = beanMap.get(id);
                if (bean == null) {
                    throw new PersistenceLuceneException("Unmatched bean " + deltaBean.getId());
                }
                deltaBean.apply(bean);    
                Document document = new Document();
                docFieldWriter.writeValue(bean, document);
                try {
                    Term term = index.createIdTerm(id);
                    indexWriter.updateDocument(term, document, analyzer);
                } catch (Exception e) {
                    throw new PersistenceLuceneException(e);
                }
            }
            
            return beanMap.keySet();
            
        } finally {
            closeResources();
        }
    }
    
    private void closeResources(){
        try {
            search.releaseClose();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error with IndexReader decRef()", e);
        }         
    }

    private LinkedHashMap<Object, Object> getBeans() {

        Query query = createQuery();

        LinkedHashMap<Object, Object> beanMap = new LinkedHashMap<Object, Object>();

        try {
            TopDocs topDocs = searcher.search(query, deltaBeans.size() * 2);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            for (int i = 0; i < scoreDocs.length; i++) {
                int doc = scoreDocs[i].doc;
                Document document = searcher.doc(doc);
                Object bean = index.readDocument(document);
                Object id = beanDescriptor.getId(bean);
                beanMap.put(id, bean);
            }

            return beanMap;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Query createQuery() {

        BooleanQuery b = new BooleanQuery();

        for (int i = 0; i < deltaBeans.size(); i++) {
            BeanDelta d = deltaBeans.get(i);
            Object id = d.getId();
            Term term = index.createIdTerm(id);
            TermQuery tq = new TermQuery(term);
            b.add(tq, Occur.SHOULD);
        }

        return b;
    }

}
