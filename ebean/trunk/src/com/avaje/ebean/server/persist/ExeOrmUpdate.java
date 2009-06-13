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
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.query.OrmUpdate;
import com.avaje.ebean.server.core.PersistRequestOrmUpdate;
import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.util.BindParamsParser;
import com.avaje.ebean.util.BindParams;

/**
 * Executes the UpdateSql requests.
 */
public class ExeOrmUpdate {

	private static final Logger logger = Logger.getLogger(ExeOrmUpdate.class.getName());
	
    private final Binder binder;
    
    private final PstmtFactory pstmtFactory;
    
    /**
     * Create with a given binder.
     */
    public ExeOrmUpdate(Binder binder) {
    	this.pstmtFactory = new PstmtFactory();
    	this.binder = binder;
    }
    
    /**
     * Execute the orm update request.
     */
    public int execute(PersistRequestOrmUpdate request) {

        ServerTransaction t = request.getTransaction();
        
        boolean batchThisRequest = t.isBatchThisRequest();
        
        PreparedStatement pstmt = null;
        try {
            
        	pstmt = bindStmt(request, batchThisRequest);
        	
            if (batchThisRequest){
            	pstmt.addBatch();
                // return -1 to indicate batch mode
                return -1;
                
            } else {
            	OrmUpdate<?> ormUpdate = request.getOrmUpdate();
            	if (ormUpdate.getTimeout() > 0){
            		pstmt.setQueryTimeout(ormUpdate.getTimeout());
            	}
            	
            	int rowCount = pstmt.executeUpdate();
                request.checkRowCount(rowCount);
                request.postExecute();
                return rowCount;
               
            }

        } catch (SQLException ex) {
        	OrmUpdate<?> ormUpdate = request.getOrmUpdate();
        	String msg = "Error executing: "+ormUpdate.getGeneratedSql();
            throw new PersistenceException(msg, ex);

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
	
    /**
     * Convert bean and property names to db table and columns.
     */
    private String translate(PersistRequestOrmUpdate request, String sql) {
    	
    	BeanDescriptor<?> descriptor = request.getBeanDescriptor();
    	return descriptor.convertOrmUpdateToSql(sql);
    }
	
    private PreparedStatement bindStmt(PersistRequestOrmUpdate request, boolean batchThisRequest) throws SQLException {
        
    	OrmUpdate<?> ormUpdate = request.getOrmUpdate();
    	ServerTransaction t = request.getTransaction();
    	
    	String sql = ormUpdate.getUpdateStatement();
    	
    	// convert bean and property names to table and 
    	// column names if required
    	sql = translate(request, sql);
    	
    	BindParams bindParams = ormUpdate.getBindParams();
        
    	// process named parameters if required
    	sql = BindParamsParser.parse(bindParams, sql);
        
    	ormUpdate.setGeneratedSql(sql);
    	
    	PreparedStatement pstmt;
    	if (batchThisRequest){
    		pstmt = pstmtFactory.getPstmt(t, sql, request);
    		
    	} else {
    		pstmt = pstmtFactory.getPstmt(t, sql);
    	}
        
        String bindLog = null;
        if (!bindParams.isEmpty()){	       
        	bindLog = binder.bind(bindParams, 0, pstmt);
        }
        
        request.setBindLog(bindLog);
        
        return pstmt;
    }

}
