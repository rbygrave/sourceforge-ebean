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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;
import com.avaje.ebeaninternal.server.cluster.BinaryMessageList;

public class RemoteTransactionEvent implements Serializable, Runnable {

    private static final long serialVersionUID = 757920022500956949L;

    private ArrayList<BeanPersistIds> beanPersistList = new ArrayList<BeanPersistIds>();
    
    private ArrayList<TableIUD> tableList = new ArrayList<TableIUD>(4);

    private String serverName;

    private transient SpiEbeanServer server;
    
    public RemoteTransactionEvent(String serverName) {
        this.serverName = serverName;
    }
    
    public RemoteTransactionEvent(SpiEbeanServer server) {
        this.server = server;
    }
    
    public void run() {
        server.remoteTransactionEvent(this);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(beanPersistList);
        sb.append(tableList);
        return sb.toString();
    }
    
    public void writeBinaryMessage(BinaryMessageList msgList) throws IOException {
        
        for (int i = 0; i < tableList.size(); i++) {
            tableList.get(i).writeBinaryMessage(msgList);
        }
        
        for (int i = 0; i < beanPersistList.size(); i++) {
            beanPersistList.get(i).writeBinaryMessage(msgList);
        }
    }
    
    public boolean isEmpty() {
        return beanPersistList.isEmpty() && tableList.isEmpty();
    }
    
    public void add(BeanPersistIds beanPersist){
        beanPersistList.add(beanPersist);
    }
    
    public void add(TableIUD tableIud){
        tableList.add(tableIud);
    }

    public String getServerName() {
        return serverName;
    }
    
    public SpiEbeanServer getServer() {
        return server;
    }

    public void setServer(SpiEbeanServer server) {
        this.server = server;
    }
    
    public List<TableIUD> getTableIUDList() {
        return tableList;
    }

    public List<BeanPersistIds> getBeanPersistList() {
        return beanPersistList;
    }
    
}
