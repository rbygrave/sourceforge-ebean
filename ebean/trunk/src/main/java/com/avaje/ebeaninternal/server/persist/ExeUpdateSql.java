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
package com.avaje.ebeaninternal.server.persist;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebeaninternal.api.BindParams;
import com.avaje.ebeaninternal.api.SpiSqlUpdate;
import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.server.core.PersistRequestUpdateSql;
import com.avaje.ebeaninternal.server.core.PstmtBatch;
import com.avaje.ebeaninternal.server.core.PersistRequestUpdateSql.SqlType;
import com.avaje.ebeaninternal.server.type.DataBind;
import com.avaje.ebeaninternal.server.util.BindParamsParser;

/**
 * Executes the UpdateSql requests.
 */
public class ExeUpdateSql {

	private static final Logger logger = Logger.getLogger(ExeUpdateSql.class.getName());
	
    private final Binder binder;
    
    private final PstmtFactory pstmtFactory;
    
    private final PstmtBatch pstmtBatch;
    
    //TODO: get defaultBatchSize
    private int defaultBatchSize = 20;
    
    /**
     * Create with a given binder.
     */
    public ExeUpdateSql(Binder binder, PstmtBatch pstmtBatch) {
    	this.binder = binder;
    	this.pstmtBatch = pstmtBatch;
    	this.pstmtFactory = new PstmtFactory(pstmtBatch);
    }
    
    /**
     * Execute the UpdateSql request.
     */
    public int execute(PersistRequestUpdateSql request) {

        SpiTransaction t = request.getTransaction();
        
        boolean batchThisRequest = t.isBatchThisRequest();
        
        PreparedStatement pstmt = null;
        try {
            
        	pstmt = bindStmt(request, batchThisRequest);
        	
            if (batchThisRequest){
            	if (pstmtBatch != null){
            		pstmtBatch.addBatch(pstmt);
            	} else {
            		pstmt.addBatch();
            	}
                // return -1 to indicate batch mode
                return -1;
                
            } else {
            	int rowCount = pstmt.executeUpdate();
                request.checkRowCount(rowCount);
                request.postExecute();
                return rowCount;
               
            }

        } catch (SQLException ex) {
            throw new PersistenceException(ex);

        } finally {
            if (!batchThisRequest && pstmt != null) {
                try {
                	pstmt.close();
                } catch (SQLException e) {
                	logger.log(Level.SEVERE, null, e);
                }
            }
        }
    }
	
	
    private PreparedStatement bindStmt(PersistRequestUpdateSql request, boolean batchThisRequest) throws SQLException {
        
    	SpiSqlUpdate updateSql = request.getUpdateSql();
    	SpiTransaction t = request.getTransaction();
    	
    	String sql = updateSql.getSql();
    	
    	BindParams bindParams = updateSql.getBindParams();
        
    	// process named parameters if required
    	sql = BindParamsParser.parse(bindParams, sql);
        
    	PreparedStatement pstmt;
    	if (batchThisRequest){
    		pstmt = pstmtFactory.getPstmt(t, sql, request);
    		if (pstmtBatch != null){
    			// oracle specific JDBC setting batch size ahead of time
    			int batchSize = t.getBatchSize();
    			if (batchSize < 1){
    				batchSize = defaultBatchSize;
    			}
    			pstmtBatch.setBatchSize(pstmt, batchSize);
    		}
    		
    	} else {
    		pstmt = pstmtFactory.getPstmt(t, sql);
    	}
    	
    	if (updateSql.getTimeout() > 0){
    		pstmt.setQueryTimeout(updateSql.getTimeout());
    	}
    	
        String bindLog = null;
        if (!bindParams.isEmpty()){	       
        	bindLog = binder.bind(bindParams, new DataBind(pstmt));
        }
        
        request.setBindLog(bindLog);
        
        // derive the statement type (for TransactionEvent)
        parseUpdate(sql, request);
        
        return pstmt;
    }

    
    private void determineType(String word1, String word2, String word3, PersistRequestUpdateSql request) {
        if (word1.equalsIgnoreCase("UPDATE")) {
        	request.setType(SqlType.SQL_UPDATE, word2, "UpdateSql");

        } else if (word1.equalsIgnoreCase("DELETE")) {
        	request.setType(SqlType.SQL_DELETE, word3, "DeleteSql");

        } else if (word1.equalsIgnoreCase("INSERT")) {
        	request.setType(SqlType.SQL_INSERT, word3, "InsertSql");

        } else {
        	request.setType(SqlType.SQL_UNKNOWN, null, "UnknownSql");

        }
    }

    private void parseUpdate(String sql, PersistRequestUpdateSql request) {
        
        int start = ltrim(sql);
        
        int[] pos = new int[3];
        int spaceCount = 0;
        
        int len = sql.length();
        for (int i = start; i < len; i++) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) {
                pos[spaceCount] = i;
                spaceCount++;
                if (spaceCount > 2){
                    break;
                }
            }
        }
        
        String firstWord = sql.substring(0, pos[0]);
        String secWord   = sql.substring(pos[0]+1, pos[1]);
        String thirdWord;
        if (pos[2] == 0){
        	// there is nothing after the table name
            thirdWord = sql.substring(pos[1]+1);
        } else {
            thirdWord = sql.substring(pos[1]+1, pos[2]);
        }
        
        determineType(firstWord, secWord, thirdWord, request);
    }
    
    private int ltrim(String s) {
        int len = s.length();
        int i = 0;
        for (i = 0; i < len; i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return 0;
    }
}
