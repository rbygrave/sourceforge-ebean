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
package com.avaje.ebeaninternal.server.lucene;


public final class LIndexWork {

    public enum WorkType {
        
        /**
         * Update based on transaction changes.
         */
        TXN_UPDATE,

        /**
         * Update based on query using DB last updated column.
         */
        QUERY_UPDATE,

        /**
         * Rebuild the entire index.
         */
        REBUILD
    }

    public static LIndexWork newRebuild(LIndexUpdateFuture future) {
        return new LIndexWork(WorkType.REBUILD, future, null);
    }
    
    public static LIndexWork newQueryUpdate(LIndexUpdateFuture future) {
        return new LIndexWork(WorkType.QUERY_UPDATE, future, null);
    }
    
    public static LIndexWork newTxnUpdate(LIndexUpdateFuture future, IndexUpdates indexUpdates) {
        return new LIndexWork(WorkType.TXN_UPDATE, future, indexUpdates);
    }
    
    private final WorkType workType;
    
    private final LIndexUpdateFuture future;
    
    private final IndexUpdates indexUpdates;
    
    private LIndexWork(WorkType workType, LIndexUpdateFuture future, IndexUpdates indexUpdates) {
        this.workType = workType;
        this.future = future;
        this.indexUpdates = indexUpdates;
    }

    public WorkType getWorkType(){
        return workType;
    }

    public IndexUpdates getIndexUpdates() {
        return indexUpdates;
    }

    public LIndexUpdateFuture getFuture() {
        return future;
    }
    
}
