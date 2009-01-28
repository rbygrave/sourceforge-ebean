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
package org.avaje.ebean.server.transaction;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a map of Table modification information.
 * Used to invalidate cache elements based on the time tables
 * where last modified. 
 * <p>
 * Holds a map or tables by name and when they where last modified
 * by a commited insert update or delete statement.
 * </p>
 */
public class TableStateManager {

	//private static final Logger logger = LogFactory.get(TableStateManager.class);
	
    /**
     * Map of TableState
     */
    private Map<String,TableState> tableStateMap;

    /**
     * Create the TableStateManager.
     */
    public TableStateManager() {
        tableStateMap = new ConcurrentHashMap<String,TableState>(500);
    }
    
    /**
     * Process the TransactionEvent.
     * Updates the table state information.
     */
	public void process(TransactionEvent event){
	    Iterator<TableModInfo> it = event.tableModInfoIterator();
	    while (it.hasNext()) {
            TableModInfo modInfo = (TableModInfo) it.next();
    		getTableState(modInfo.getTableName()).update(modInfo);
        }
	}
	
	/**
	 * Returns the TableState for a given table.
	 */
	public TableState getTableState(String tableName){
		
	    // make tableName case insensitive using toUpperCase
	    tableName = tableName.toUpperCase();
	    
		TableState state = (TableState)tableStateMap.get(tableName);
		if (state != null){
			return state;
		}
		synchronized(tableStateMap){
			state = (TableState)tableStateMap.get(tableName);
			if (state != null){
				return state;
			}
			state = new TableState(tableName);
            tableStateMap.put(tableName, state);
			return state;
		}
	}
	
    /**
     * Return an Iterator of the TableState's.
     */
	public Iterator<TableState> getTableStateIterator(){
		return tableStateMap.values().iterator();
	}

	/**
	 * Set all the tables to have a modified now time.
	 * This essentially will invalidate anything that is cached dependant
	 * on the tables.  All Lookups and beans cached will be invalidated.
	 */
	public void setAllTablesModifiedNow(){
		synchronized(tableStateMap){
			Iterator<TableState> i = getTableStateIterator();
			while (i.hasNext()){
				TableState tableState = (TableState)i.next();
				tableState.setModifiedNow();
			}
		}
	}


}
