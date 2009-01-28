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

import java.util.Iterator;
import java.util.List;

import javax.persistence.PersistenceException;

import org.avaje.ebean.Ebean;
import org.avaje.ebean.MapBean;
import org.avaje.ebean.SqlQuery;
import org.avaje.ebean.server.core.InternalEbeanServer;
import org.avaje.ebean.server.deploy.BeanDescriptor;
import org.avaje.ebean.server.deploy.BeanProperty;

/**
 * Utility program that updartes the max values in a the sequence table.
 */
public class TableSequenceUpdateMax {

    /**
     * update the sequence table with the max from
     */
    public static void main(String[] args) {

        TableSequenceUpdateMax m = new TableSequenceUpdateMax();
        m.run();
    }

    public void run() {

        List<SequenceBean> list = Ebean.createQuery(SequenceBean.class).findList();
        
        Iterator<SequenceBean> it = list.iterator();
        while (it.hasNext()) {
            SequenceBean sequenceBean = it.next();
            update(sequenceBean);
        }
    }

    public void update(SequenceBean sequenceBean) {
        
        String tableName = sequenceBean.getName();

        InternalEbeanServer primaryServer = (InternalEbeanServer)Ebean.getServer(null);
        BeanDescriptor desc = primaryServer.getMapBeanDescriptor(tableName);
        
        BeanProperty[] uids = desc.propertiesId();
        if (uids.length != 1) {
        	String msg = "Table [" + tableName + "] has [" + uids.length + "] unique columns?";
            throw new PersistenceException(msg);        	
        }

        for (int i = 0; i < uids.length; i++) {
			
        	BeanProperty idProp = uids[i];

            String sql = "select max(" + idProp.getDbColumn() + ") as maxid from " + tableName;

            SqlQuery sqlQuery = Ebean.createSqlQuery();
            sqlQuery.setQuery(sql);
            
            List<MapBean> list = sqlQuery.findList();
            
            MapBean maxBean = list.get(0);
            Integer maxId = maxBean.getInteger("maxid");

            int maxInt = maxId.intValue();
            if (maxInt > sequenceBean.getNextId().intValue()) {
                sequenceBean.setNextId(Integer.valueOf(maxInt));
                Ebean.save(sequenceBean);
            }
        }

    }
}
