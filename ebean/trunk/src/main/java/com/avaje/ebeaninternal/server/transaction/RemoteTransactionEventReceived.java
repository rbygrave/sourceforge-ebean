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
package com.avaje.ebeaninternal.server.transaction;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;

public class RemoteTransactionEventReceived {

    private ArrayList<RemoteBeanPersist> beanPersistList;
    
    private ArrayList<TableIUD> tableList;
    
    private final SpiEbeanServer server;
    
    public RemoteTransactionEventReceived(SpiEbeanServer server) {
        this.server = server;
    }
    
    public void add(RemoteBeanPersist beanPersist){
        if (beanPersistList == null){
            beanPersistList = new ArrayList<RemoteBeanPersist>();
        }
        beanPersistList.add(beanPersist);
    }
    
    public void add(TableIUD tableIud){
        if (tableList == null){
            tableList = new ArrayList<TableIUD>();
        }
        tableList.add(tableIud);
    }

    public SpiEbeanServer getServer() {
        return server;
    }

    public List<TableIUD> getTableIUDList() {
        return tableList;
    }

    public List<RemoteBeanPersist> getBeanPersistList() {
        return beanPersistList;
    }
    
}
