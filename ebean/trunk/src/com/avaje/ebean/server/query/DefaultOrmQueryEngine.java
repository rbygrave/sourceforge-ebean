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
package com.avaje.ebean.server.query;

import java.util.ArrayList;

import com.avaje.ebean.bean.BeanFinder;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.core.OrmQueryEngine;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.core.ServerCache;
import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.core.TransactionContext;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.DeploymentManager;
import com.avaje.ebean.server.plugin.Plugin;

/**
 * Main Finder implementation.
 */
public class DefaultOrmQueryEngine implements OrmQueryEngine {

    /**
     * Find using predicates
     */
    private final CQueryEngine queryEngine;
    
	private final ServerCache serverCache;

    private final DeploymentManager deploymentManager;
    
    /**
     * Create the Finder.
     */
    public DefaultOrmQueryEngine(Plugin plugin, InternalEbeanServer server) {
   
        queryEngine = server.getQueryEngine();
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
        	result = queryEngine.findMany(request);
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
        	result = queryEngine.find(request);
        }
        
        if (query.isUseCache()){
        	request.putToCacheMany(result);
        }
        
        return result;
    }


}
