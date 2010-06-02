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
import java.util.List;
import java.util.Set;

import javax.persistence.PersistenceException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.IndexSearcher;

import com.avaje.ebean.config.lucene.LuceneIndex;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryDetail;
import com.avaje.ebeaninternal.server.transaction.BeanDelta;

public class LIndex implements LuceneIndex {

    private final String indexName;
    
    private final Analyzer analyzer;
    
    private final MaxFieldLength maxFieldLength;
    
    private final LIndexFields fieldDefn;
    
    private final BeanDescriptor<?> desc;

    private final OrmQueryDetail ormQueryDetail;
    
    private final LIndexIo indexIo;

    private final LIndexFieldId idField;
    
    public LIndex(LuceneIndexManager manager, String indexName, String indexDir, Analyzer analyzer, MaxFieldLength maxFieldLength,
            BeanDescriptor<?> desc, LIndexFields fieldDefn) throws IOException {
        
        this.indexName = indexName;
        this.analyzer = analyzer;
        this.maxFieldLength = maxFieldLength;
        this.desc = desc;
        this.fieldDefn = fieldDefn;
        this.idField = fieldDefn.getIdField();
        this.ormQueryDetail = fieldDefn.getOrmQueryDetail();
        
        this.indexIo = new LIndexIo(manager, indexDir, this); 
        manager.addIndex(this);
        fieldDefn.registerIndexWithProperties(this);
    }
    
    public void manage(LuceneIndexManager indexManager) {
        indexIo.manage(indexManager);
    }
    
    public Term createIdTerm(Object id) {
        return idField.createTerm(id);
    }
    
    public void shutdown() {
        indexIo.shutdown();
    }
    
    public int rebuild() {
        try {
            return indexIo.rebuild();
        } catch (IOException e){
            throw new PersistenceException(e);
        }
    }
    
    public int update() {
        return rebuild();
    }

    public String toString() {
        return getDefnName();
    }
    
    public String getDefnName() {
        return indexName;
    }
    
    public Class<?> getBeanType() {
        return desc.getBeanType();
    }

    public BeanDescriptor<?> getBeanDescriptor() {
        return desc;
    }

    public IndexSearcher getIndexSearcher() {
        return indexIo.getIndexSearcher();
    }
    
    public Analyzer getAnalyzer() {
        return analyzer;
    }
    
    public MaxFieldLength getMaxFieldLength() {
        return maxFieldLength;
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

    public void process(List<BeanDelta> deltaBeans) {
        LIndexDeltaHandler h = indexIo.createDeltaHandler(deltaBeans);
        h.process();
        indexIo.queueCommit();
    }

}
