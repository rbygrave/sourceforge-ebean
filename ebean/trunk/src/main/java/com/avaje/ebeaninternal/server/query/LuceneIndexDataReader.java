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
package com.avaje.ebeaninternal.server.query;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import javax.persistence.PersistenceException;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;

import com.avaje.ebeaninternal.server.core.LuceneOrmQueryRequest;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.lucene.LIndex;
import com.avaje.ebeaninternal.server.lucene.LIndexField;
import com.avaje.ebeaninternal.server.lucene.LIndexFields;
import com.avaje.ebeaninternal.server.type.DataReader;

public class LuceneIndexDataReader implements DataReader {

    private final LIndexFields indexFieldDefn;

    private LIndexField[] readFields;

    private ScoreDoc[] scoreDocs;
    
    private Searcher searcher;
    
    private int maxReadRows;

    private int colIndex;

    private int rowIndex;
    
    private Document currentDoc;
    
    
    public LuceneIndexDataReader(OrmQueryRequest<?> request) {
        
        LIndex luceneIndex = request.getLuceneIndex();
        
        this.indexFieldDefn = luceneIndex.getIndexFieldDefn();
        readFields = indexFieldDefn.getReadFields();
        this.searcher = luceneIndex.getIndexSearcher();

        LuceneOrmQueryRequest luceneRequest = request.getLuceneOrmQueryRequest();
        
        int maxRows = request.getQuery().getMaxRows();
        if (maxRows < 1){
            maxRows = 100;
        }
        org.apache.lucene.search.Query luceneQuery = luceneRequest.getLuceneQuery();
        Sort luceneSort = luceneRequest.getLuceneSort();
        try {
            TopDocs topDocs;
            if (luceneSort == null){
                topDocs = searcher.search(luceneQuery, null, maxRows);
            } else {
                topDocs = searcher.search(luceneQuery, null, maxRows, luceneSort);
            }
            
            scoreDocs = topDocs.scoreDocs;
            maxReadRows = scoreDocs.length;

        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }

    public byte[] readFieldAsBytes() {
        try {
            String fieldName = readFields[colIndex++].getName();
            return currentDoc.getBinaryValue(fieldName);
        
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }
    
    public String readFieldAsString() {
        try {
            String fieldName = readFields[colIndex++].getName();
            return currentDoc.get(fieldName);
        
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }
    
    public void close() throws SQLException {
        try {
            searcher.close();
        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }

    public Array getArray() throws SQLException {
        throw new PersistenceException("Not Supported yet");
    }

    public BigDecimal getBigDecimal() throws SQLException {
        String s = readFieldAsString();
        return s == null ? null : new BigDecimal(s);
    }

    public byte[] getBinaryBytes() throws SQLException {
        return readFieldAsBytes();
    }

    public byte[] getBlobBytes() throws SQLException {
        return readFieldAsBytes();
    }

    public Boolean getBoolean() throws SQLException {
        String s = readFieldAsString();
        return s == null ? null : Boolean.valueOf(s);
    }

    public Byte getByte() throws SQLException {
        byte[] bytes = readFieldAsBytes();
        return bytes == null ? null : bytes[0];
    }

    public byte[] getBytes() throws SQLException {
        return readFieldAsBytes();
    }

    public Date getDate() throws SQLException {
        
        Long longVal = getLong();
        return longVal == null ? null : new Date(longVal);
    }

    public Double getDouble() throws SQLException {
        String s = readFieldAsString();
        return s == null ? null : Double.valueOf(s);
    }

    public Float getFloat() throws SQLException {
        String s = readFieldAsString();
        return s == null ? null : Float.valueOf(s);
    }

    public Integer getInt() throws SQLException {
        String s = readFieldAsString();
        return s == null ? null : Integer.valueOf(s);
    }

    public Long getLong() throws SQLException {
        String s = readFieldAsString();
        return s == null ? null : Long.valueOf(s);
    }

    public Short getShort() throws SQLException {
        String s = readFieldAsString();
        return s == null ? null : Short.valueOf(s);
    }

    public String getString() throws SQLException {
        return readFieldAsString();
    }

    public String getStringClob() throws SQLException {
        return readFieldAsString();
    }

    public String getStringFromStream() throws SQLException {
        return readFieldAsString();
    }

    public Time getTime() throws SQLException {
        String s = readFieldAsString();
        return s == null ? null : Time.valueOf(s);
    }

    public Timestamp getTimestamp() throws SQLException {
        Long longVal = getLong();
        return longVal == null ? null : new Timestamp(longVal);
    }

    public void incrementPos(int increment) {
        colIndex += increment;
    }

    public boolean next() throws SQLException {
        if (rowIndex >= maxReadRows) {
            return false;
        }
        try {
            int docIndex = scoreDocs[rowIndex++].doc;
            currentDoc = searcher.doc(docIndex);            
            return true;
            
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    public void resetColumnPosition() {
        colIndex = 0;
    }

    
}
