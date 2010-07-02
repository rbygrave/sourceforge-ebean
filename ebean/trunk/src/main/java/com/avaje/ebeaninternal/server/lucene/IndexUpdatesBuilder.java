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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.transaction.BeanDeltaList;
import com.avaje.ebeaninternal.server.transaction.BeanPersistIds;
import com.avaje.ebeaninternal.server.transaction.IndexInvalidate;
import com.avaje.ebeaninternal.server.transaction.RemoteTransactionEvent;

public class IndexUpdatesBuilder {

    private final SpiEbeanServer server;

    private final Map<String, IndexUpdates> map;

    private final RemoteTransactionEvent txnEvent;

    public static Collection<IndexUpdates> create(SpiEbeanServer server, RemoteTransactionEvent txnEvent) {
        return new IndexUpdatesBuilder(server, txnEvent).create();
    }
    
    private IndexUpdatesBuilder(SpiEbeanServer server, RemoteTransactionEvent txnEvent) {
        this.map = new HashMap<String, IndexUpdates>();
        this.server = server;
        this.txnEvent = txnEvent;
    }

    private Collection<IndexUpdates> create() {

        Set<IndexInvalidate> indexInvalidations = txnEvent.getIndexInvalidations();
        if (indexInvalidations != null){
            for (IndexInvalidate indexInvalidate : indexInvalidations) {
                LIndex index = server.getLuceneIndexManager().getIndex(indexInvalidate.getIndexName());
                BeanDescriptor<?> d = index.getBeanDescriptor();
                getEventByType(d).setInvalidate(true);
            }
        }
        
        List<TableIUD> tableIUDList = txnEvent.getTableIUDList();
        if (tableIUDList != null) {
            for (int i = 0; i < tableIUDList.size(); i++) {
                TableIUD tableIUD = tableIUDList.get(i);
                List<BeanDescriptor<?>> descList = server.getBeanDescriptors(tableIUD.getTable());
                if (descList != null) {
                    for (int j = 0; j < descList.size(); j++) {
                        BeanDescriptor<?> d = descList.get(j);
                        getEventByType(d).addTableIUD(tableIUD);
                    }
                }
            }
        }

        List<BeanPersistIds> beanPersistList = txnEvent.getBeanPersistList();
        if (beanPersistList != null) {
            for (int i = 0; i < beanPersistList.size(); i++) {
                BeanPersistIds b = beanPersistList.get(i);
                getEventByType(b.getBeanDescriptor()).setBeanPersistIds(b);
            }
        }

        List<BeanDeltaList> beanDeltaLists = txnEvent.getBeanDeltaLists();
        if (beanDeltaLists != null) {
            for (int i = 0; i < beanDeltaLists.size(); i++) {
                BeanDeltaList d = beanDeltaLists.get(i);
                getEventByType(d.getBeanDescriptor()).setDeltaList(d);
            }
        }

        return map.values();
    }

    private IndexUpdates getEventByType(BeanDescriptor<?> d) {

        String beanDescKey = d.getBeanType().getName();
        IndexUpdates eventByType = map.get(beanDescKey);
        if (eventByType == null) {
            eventByType = new IndexUpdates(d);
            map.put(beanDescKey, eventByType);
        }
        return eventByType;
    }

}
