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
package org.avaje.ebean.server.core;

import java.util.ArrayList;

import org.avaje.ebean.CallableSql;
import org.avaje.ebean.ServerConfiguration;
import org.avaje.ebean.SqlUpdate;
import org.avaje.ebean.server.transaction.TransactionEvent;
import org.avaje.ebean.util.BindParams;

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
    public static TransactionEvent getTransactionEvent(CallableSql callSql){
        return pa.getTransactionEvent(callSql);
    }
     
    /**
     * Return the classes (entities, embeddable, finders, scalar types etc). 
     */
    public static ArrayList<Class<?>> getClasses(ServerConfiguration serverConfig) {
    	return pa.getClasses(serverConfig);
    }
        
}
