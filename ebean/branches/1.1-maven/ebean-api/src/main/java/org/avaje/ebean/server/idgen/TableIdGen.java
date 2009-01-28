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
package org.avaje.ebean.server.idgen;

import java.util.HashMap;

import org.avaje.ebean.server.core.InternalEbeanServer;
import org.avaje.ebean.server.core.IdGenerator;
import org.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * IdGenerator based on a database table.
 */
public class TableIdGen implements IdGenerator {

    /**
     * The map of sequences.
     */
    HashMap<String,TableSequence> seqMap = new HashMap<String, TableSequence>();
    
    /**
     * The implementation database.
     */
    InternalEbeanServer server;
    
    //String name;
    
    /**
     * Create the TableIdGen.
     */
    public TableIdGen(){
    }
    
    public void configure(String name, InternalEbeanServer idServer) {
        //this.name =  name;
        this.server = idServer;
    }

    /**
     * Get the next Id for a given bean type.
     */
    public Object nextId(BeanDescriptor beanType) {
        
        String tableName = beanType.getBaseTable();       
        return nextId(tableName);
    }

    /**
     * Get the next id for a given table name.
     */
    public Object nextId(String tableName) {
        
        // make it case insensitive
        tableName = tableName.toLowerCase();
        
        TableSequence tabSeq = getTableSequence(tableName);
        int nextInt = tabSeq.next();
        
        return Integer.valueOf(nextInt);
    }

    private TableSequence getTableSequence(String tableName){
        
    	synchronized (seqMap) {
	        TableSequence tabSeq = (TableSequence)seqMap.get(tableName);
            if (tabSeq == null){
                tabSeq = new TableSequence(tableName, server);
                seqMap.put(tableName,tabSeq);
            }
        
	        return tabSeq;
	    }
    }
}
