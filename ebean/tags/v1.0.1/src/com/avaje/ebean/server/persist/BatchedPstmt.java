/**
 * Copyright (C) 2006  Robin Bygrave
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
package com.avaje.ebean.server.persist;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * A batched statement that is held in BatchedPstmtHolder. It has a list of
 * BatchPostExecute which it will process after the statement is executed.
 * <p>
 * This can hold CallableStatements as well.
 * </p>
 */
public class BatchedPstmt {

    /**
     * The underlying statement.
     */
    PreparedStatement pstmt;
    
    /**
     * True if an insert that uses generated keys.
     */
    boolean isGenKeys;
    
    /**
     * The list of BatchPostExecute used to perform post processing.
     */
    ArrayList<BatchPostExecute> list = new ArrayList<BatchPostExecute>();
    
    String sql;
    
    /**
     * Create with a given statement.
     * @param isGenKeys true if an insert that uses generatedKeys
     */
    public BatchedPstmt(PreparedStatement pstmt, boolean isGenKeys, String sql) {
        this.pstmt = pstmt;
        this.isGenKeys = isGenKeys;
        this.sql = sql;
    }
    
    /**
     * Return the number of batched statements.
     */
    public int size() {
    	return list.size();
    }
    
    /**
     * Return the sql
     */
    public String getSql() {
    	return sql;
    }

	/**
     * Return the statement.
     */
    public PreparedStatement getStatement() {
        return pstmt;
    }
    
    /**
     * Add the BatchPostExecute to the list for post execute processing.
     */
    public void add(BatchPostExecute batchExecute){
        list.add(batchExecute);
    }
    
    /**
     * Execute the statement using executeBatch().
     * Run any post processing including getGeneratedKeys.
     */
    public void executeBatch(boolean getGeneratedKeys) throws SQLException {
    	executeAndCheckRowCounts();
        if (isGenKeys && getGeneratedKeys){
            getGeneratedKeys();
        }
        postExecute();
        close();
    }
    
    /**
     * Close the underlying statement.
     */
    public void close() throws SQLException {
        if (pstmt != null){
            pstmt.close();
            pstmt = null;
        }
    }
    
    private void postExecute() throws SQLException {
    	for (int i = 0; i < list.size(); i++) {
            BatchPostExecute batchExecute = (BatchPostExecute)list.get(i);
            batchExecute.postExecute();
        }
    }
    
    private void executeAndCheckRowCounts() throws SQLException {

        int[] results = pstmt.executeBatch();
        
        if (results.length != list.size()){
            String s = "results array error "+results.length+" "+list.size();
            throw new SQLException(s);
        }
        
        // check for concurrency exceptions...
        for (int i = 0; i < results.length; i++) {
            getBatchExecute(i).checkRowCount(results[i]);
        }
    }
    
    private void getGeneratedKeys() throws SQLException {
        
        int index = 0;
        ResultSet rset = pstmt.getGeneratedKeys();
        try {
            while(rset.next()) {
                Object idValue = rset.getObject(1);
                getBatchExecute(index).setGeneratedKey(idValue);
                index++;
            } 
        } finally {
            if (rset != null){
                rset.close();
            }
        }
    }
    
    
    private BatchPostExecute getBatchExecute(int i) {
        return (BatchPostExecute)list.get(i);
    }
}
