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
package org.avaje.ebean.server.query;

import java.util.ArrayList;

import org.avaje.ebean.bean.BeanFinder;
import org.avaje.ebean.bean.EntityBean;
import org.avaje.ebean.query.OrmQuery;
import org.avaje.ebean.server.core.InternalEbeanServer;
import org.avaje.ebean.server.core.OrmQueryEngine;
import org.avaje.ebean.server.core.QueryRequest;
import org.avaje.ebean.server.core.ServerCache;
import org.avaje.ebean.server.core.ServerTransaction;
import org.avaje.ebean.server.core.TransactionContext;
import org.avaje.ebean.server.deploy.BeanDescriptor;
import org.avaje.ebean.server.deploy.DeploymentManager;
import org.avaje.ebean.server.plugin.Plugin;

/**
 * Main Finder implementation.
 */
public class DefaultOrmQueryEngine implements OrmQueryEngine {

    /**
     * Find using predicates
     */
    private final DefaultOrmQueryEngineHelper findObjRel;
    
	private final ServerCache serverCache;

    private final DeploymentManager deploymentManager;
    
    /**
     * Create the Finder.
     */
    public DefaultOrmQueryEngine(Plugin plugin, InternalEbeanServer server) {
   
        findObjRel = new DefaultOrmQueryEngineHelper(plugin.getPluginCore());
        serverCache = server.getServerCache();
        deploymentManager = plugin.getPluginCore().getDeploymentManager();
    }
    
    
    public Object findMany(QueryRequest request) {

    	Object result = request.getFromCache(serverCache);
    	if (result != null){
    		return result;
    	}

        ServerTransaction t = request.getTransaction();
        
        // before we perform a query, we need to flush any
        // previous persist requests that are queued/batched.
        // The query may read data affected by those requests.
        t.batchFlush();
        
        OrmQuery<?> query = request.getQuery();
        ArrayList<EntityBean> adds = query.getContextAdditions();
        if (adds != null){
            TransactionContext pc = t.getTransactionContext();
            for (int i = 0; i < adds.size(); i++) {
            	EntityBean bean = adds.get(i);
            	BeanDescriptor desc = deploymentManager.getBeanDescriptor(bean.getClass());
            	Object id = desc.getId(bean);
            	pc.add(bean, id, false);
			}
        }

        BeanFinder finder = request.getBeanFinder();
        if (finder != null) {
            // this bean type has its own specific finder
            result = finder.findMany(request);
        } else {
        	result = findObjRel.findMany(request);
        }

        if (query.isUseCache()){
        	request.putToCacheMany(result);
        }
        
        return result;
    }


    /**
     * Find a single bean using its unique id.
     */
    public Object findId(QueryRequest request) {
        
    	Object result = request.getFromCache(serverCache);
    	if (result != null){
    		return result;
    	}
        
        ServerTransaction t = request.getTransaction();
        
        if (t.isBatchFlushOnQuery()){
            // before we perform a query, we need to flush any
            // previous persist requests that are queued/batched.
            // The query may read data affected by those requests.
        	t.batchFlush();
        }

        OrmQuery<?> query = request.getQuery();

        ArrayList<EntityBean> adds = query.getContextAdditions();
        if (adds != null){
            TransactionContext pc = t.getTransactionContext();
            for (int i = 0; i < adds.size(); i++) {
            	EntityBean bean = adds.get(i);
            	BeanDescriptor desc = deploymentManager.getBeanDescriptor(bean.getClass());
            	Object id = desc.getId(bean);
            	pc.add(bean, id, false);
			}
        }
        
        BeanFinder finder = request.getBeanFinder();
        if (finder != null) {
            result =  finder.find(request);
        } else {
        	result = findObjRel.find(request);
        }
        
        if (query.isUseCache()){
        	request.putToCacheMany(result);
        }
        
        return result;
    }


}
