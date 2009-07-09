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
package com.avaje.ebean;

import com.avaje.ebean.bean.BindParams;
import com.avaje.ebean.server.core.ProtectedMethodAPI;
import com.avaje.ebean.server.transaction.TransactionEventTable;

/**
 * Implements the public API protection.
 */
class ProtectedMethodImpl implements ProtectedMethodAPI {

    /**
     * Return the BindParams for a UpdateSql.
     */
    public BindParams getBindParams(SqlUpdate updSql) {
        return updSql.getBindParams();
    }
    
    /**
     * Return the BindParams for a CallableSql.
     */
    public BindParams getBindParams(CallableSql callSql) {
        return callSql.getBindParams();
    }

    
    /**
     * Return the TransactionEvent for a CallableSql.
     */
    public TransactionEventTable getTransactionEventTable(CallableSql callSql) {
        return callSql.getTransactionEventTable();
    }
    
}
