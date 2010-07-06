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

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.transaction.BeanDeltaList;
import com.avaje.ebeaninternal.server.transaction.BeanPersistIds;

public class IndexUpdates {

    private final BeanDescriptor<?> beanDescriptor;
    
    private List<TableIUD> tableList;

    private BeanPersistIds deleteIds;

    private BeanPersistIds beanPersistIds;
    
    private BeanDeltaList deltaList;
    
    private boolean invalidate;
    
    public IndexUpdates(BeanDescriptor<?> beanDescriptor) {
        this.beanDescriptor = beanDescriptor;
    }
    
    public BeanDescriptor<?> getBeanDescriptor() {
        return beanDescriptor;
    }

    public boolean isInvalidate() {
        return invalidate;
    }

    public void setInvalidate(boolean invalidate) {
        this.invalidate = invalidate;
    }

    public void addTableIUD(TableIUD tableIud){
        if (tableList == null){
            tableList = new ArrayList<TableIUD>(4);
        }
        tableList.add(tableIud);
    }

    public List<TableIUD> getTableList() {
        return tableList;
    }

    public void setTableList(List<TableIUD> tableList) {
        this.tableList = tableList;
    }

    public BeanPersistIds getBeanPersistIds() {
        return beanPersistIds;
    }

    public void setBeanPersistIds(BeanPersistIds beanPersistIds) {
        this.beanPersistIds = beanPersistIds;
    }
    
    public BeanPersistIds getDeleteIds() {
        return deleteIds;
    }

    public void setDeleteIds(BeanPersistIds deleteIds) {
        this.deleteIds = deleteIds;
    }

    public BeanDeltaList getDeltaList() {
        return deltaList;
    }

    public void setDeltaList(BeanDeltaList deltaList) {
        this.deltaList = deltaList;
    }
    
}
