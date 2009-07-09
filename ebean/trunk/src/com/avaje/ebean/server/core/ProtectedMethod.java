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
package com.avaje.ebean.server.core;

import com.avaje.ebean.CallableSql;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.bean.BindParams;
import com.avaje.ebean.server.transaction.TransactionEventTable;

/**
 * Access to the methods hidden by public API protection.
 */
public class ProtectedMethod {

    private static ProtectedMethodAPI pa;
    
    /**
     * Set the implementation.
     */
    public static void setPublicAccess(ProtectedMethodAPI publicAccess){
        pa = publicAccess;
    }
    
    /**
     * Return the BindParams for a UpdateSql.
     */
    public static BindParams getBindParams(SqlUpdate updSql){
        return pa.getBindParams(updSql);
    }
    
    /**
     * Return the BindParams for a CallableSql.
     */
    public static BindParams getBindParams(CallableSql callSql) {
        return pa.getBindParams(callSql);
    }
    
    /**
     * Return the TransactionEvent for a CallableSql.
     */
    public static TransactionEventTable getTransactionEventTable(CallableSql callSql){
        return pa.getTransactionEventTable(callSql);
    }
        
}
