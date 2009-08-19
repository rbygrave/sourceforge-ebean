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

import java.util.List;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.event.BeanFinder;
import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.internal.SpiTransaction;
import com.avaje.ebean.server.core.OrmQueryEngine;
import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptorManager;

/**
 * Main Finder implementation.
 */
public class DefaultOrmQueryEngine implements OrmQueryEngine {

    /**
     * Find using predicates
     */
    private final CQueryEngine queryEngine;
    
//    private final BeanDescriptorManager beanDescriptorManager;
    
    /**
     * Create the Finder.
     */
    public DefaultOrmQueryEngine(BeanDescriptorManager descMgr, CQueryEngine queryEngine) {
   
        this.queryEngine = queryEngine;
//        this.beanDescriptorManager = descMgr;
    }
    
    public <T> int findRowCount(OrmQueryRequest<T> request){
    	
    	return queryEngine.findRowCount(request);
    }

    public <T> List<Object> findIds(OrmQueryRequest<T> request){
    	
    	return queryEngine.findIds(request);
    }

    
	public <T> BeanCollection<T> findMany(OrmQueryRequest<T> request) {

    	BeanCollection<T> result = null;
    	
        SpiQuery<T> query = request.getQuery();

        if (query.isUseCache()){
        	result = request.getFromQueryCache();
        	if (result != null){
        		return result;
        	}
        }
 
        SpiTransaction t = request.getTransaction();
        
        // before we perform a query, we need to flush any
        // previous persist requests that are queued/batched.
        // The query may read data affected by those requests.
        t.batchFlush();
        
//        ArrayList<EntityBean> adds = query.getContextAdditions();
//        if (adds != null){
//            PersistenceContext pc = t.getPersistenceContext();
//            for (int i = 0; i < adds.size(); i++) {
//            	EntityBean bean = adds.get(i);
//            	BeanDescriptor<?> desc = beanDescriptorManager.getBeanDescriptor(bean.getClass());
//            	Object id = desc.getId(bean);
//            	pc.add(bean, id);//, false);
//			}
//        }

        BeanFinder<T> finder = request.getBeanFinder();
        if (finder != null) {
            // this bean type has its own specific finder
            result = finder.findMany(request);
        } else {
        	result = queryEngine.findMany(request);
        }

        if (query.isUseCache() && !result.isEmpty()){
        	request.putToQueryCache(result);
        }
        
        return result;
    }


    /**
     * Find a single bean using its unique id.
     */
	public <T> T findId(OrmQueryRequest<T> request) {
        
		T result = null;
		
		SpiQuery<T> query = request.getQuery();
		
		boolean useBeanCache = query.isUseCache() && query.getId() != null; 
		if (useBeanCache){
			result = request.getFromBeanCache();
			if (result != null){
				return result;
			}
		}
        
        SpiTransaction t = request.getTransaction();
        
        if (t.isBatchFlushOnQuery()){
            // before we perform a query, we need to flush any
            // previous persist requests that are queued/batched.
            // The query may read data affected by those requests.
        	t.batchFlush();
        }

//        ArrayList<EntityBean> adds = query.getContextAdditions();
//        if (adds != null){
//            PersistenceContext pc = t.getPersistenceContext();
//            for (int i = 0; i < adds.size(); i++) {
//            	EntityBean bean = adds.get(i);
//            	BeanDescriptor<?> desc = beanDescriptorManager.getBeanDescriptor(bean.getClass());
//            	Object id = desc.getId(bean);
//            	pc.add(bean, id);//, false);
//			}
//        }
        
        BeanFinder<T> finder = request.getBeanFinder();
        if (finder != null) {
            result =  finder.find(request);
        } else {
        	result = queryEngine.find(request);
        }
        
        if (useBeanCache && result != null){
        	request.getBeanDescriptor().cachePut(result);
        }
        
        return result;
    }


}
