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
package org.avaje.ebean.server.deploy;

import org.avaje.ebean.server.deploy.meta.DeployTableJoinColumn;

/**
 * A join pair of local and foreign properties.
 */
public class TableJoinColumn {

    /**
     * The local database column name.
     */
    private final String localDbColumn;

    /**
     * The foreign database column name.
     */
    private final String foreignDbColumn;

    /**
     * Create the pair.
     */
    public TableJoinColumn(DeployTableJoinColumn deploy) {
    	this.localDbColumn = deploy.getLocalDbColumn();
    	this.foreignDbColumn = deploy.getForeignDbColumn();
    }
    
    public TableJoinColumn(String localDbColumn, String foreignDbColumn) {
    	this.localDbColumn = localDbColumn;
    	this.foreignDbColumn = foreignDbColumn;
    }
    
//    /**
//     * Create a TableJoinColumn with the local and foreign columns swapped.
//     */
//    public TableJoinColumn createInverse() {
//    	return new TableJoinColumn(foreignDbColumn, localDbColumn);
//    }

    public String toString() {
        return localDbColumn+" = "+foreignDbColumn;
    }


    /**
     * Return the foreign database column name.
     */
    public String getForeignDbColumn() {
        return foreignDbColumn;
    }

    /**
     * Return the local database column name.
     */
    public String getLocalDbColumn() {
        return localDbColumn;
    }

}
