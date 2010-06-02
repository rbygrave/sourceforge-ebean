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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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

import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.transaction.BeanDelta;

public class LIndexDeltaHandler {

    private static final Logger logger = Logger.getLogger(LIndexDeltaHandler.class.getName());
    
    private final LIndex index;

    private final IndexSearcher searcher;

    private final IndexWriter indexWriter;

    private final Analyzer analyzer;

    private final BeanDescriptor<?> beanDescriptor;

    private final DocFieldWriter docFieldWriter;

    private final List<BeanDelta> deltaBeans;

    private final Document document = new Document();

    public LIndexDeltaHandler(LIndex index, IndexSearcher searcher, IndexWriter indexWriter, Analyzer analyzer,
            BeanDescriptor<?> beanDescriptor, DocFieldWriter docFieldWriter, List<BeanDelta> deltaBeans) {

        this.index = index;
        this.searcher = searcher;
        this.indexWriter = indexWriter;
        this.analyzer = analyzer;
        this.beanDescriptor = beanDescriptor;
        this.docFieldWriter = docFieldWriter;
        this.deltaBeans = deltaBeans;
    }

    public void process() {

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
                docFieldWriter.writeValue(bean, document);
                try {
                    Term term = index.createIdTerm(id);
                    indexWriter.updateDocument(term, document, analyzer);
                } catch (Exception e) {
                    throw new PersistenceLuceneException(e);
                }
            }
            
        } finally {
            closeResources();
        }
    }
    
    private void closeResources(){
        try {
            searcher.getIndexReader().decRef();
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