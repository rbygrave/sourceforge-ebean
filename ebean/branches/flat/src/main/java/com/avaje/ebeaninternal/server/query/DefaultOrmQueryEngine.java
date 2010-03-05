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
package com.avaje.ebeaninternal.server.query;

import java.util.Collection;
import java.util.Iterator;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.event.BeanFinder;
import com.avaje.ebeaninternal.api.BeanIdList;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.server.core.CopyBeanCollection;
import com.avaje.ebeaninternal.server.core.OrmQueryEngine;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptorManager;

/**
 * Main Finder implementation.
 */
public class DefaultOrmQueryEngine implements OrmQueryEngine {

    /**
     * Find using predicates
     */
    private final CQueryEngine queryEngine;
      
    /**
     * Create the Finder.
     */
    public DefaultOrmQueryEngine(BeanDescriptorManager descMgr, CQueryEngine queryEngine) {
   
        this.queryEngine = queryEngine;
    }
    
    public <T> int findRowCount(OrmQueryRequest<T> request){
    	
    	return queryEngine.findRowCount(request);
    }

    public <T> BeanIdList findIds(OrmQueryRequest<T> request){
    	
    	return queryEngine.findIds(request);
    }

    
	public <T> BeanCollection<T> findMany(OrmQueryRequest<T> request) {

        SpiQuery<T> query = request.getQuery();
		if (query.isUseQueryCache() || query.isLoadBeanCache()){
			// by default treat as sharedInstance and copy to 
			// mutable object later if necessary
			query.setSharedInstance();
		}
		
    	BeanCollection<T> result = null;
 
        SpiTransaction t = request.getTransaction();
        
        // before we perform a query, we need to flush any
        // previous persist requests that are queued/batched.
        // The query may read data affected by those requests.
        t.flushBatch();

        BeanFinder<T> finder = request.getBeanFinder();
        if (finder != null) {
            // this bean type has its own specific finder
            result = finder.findMany(request);
        } else {
        	result = queryEngine.findMany(request);
        }

    	if (query.isLoadBeanCache()){
        	// load the individual beans into the bean cache
        	BeanDescriptor<T> descriptor = request.getBeanDescriptor();
        	Collection<T> c  = result.getActualDetails();
        	Iterator<T> it = c.iterator();
        	while (it.hasNext()) {
				T bean = it.next();
				if (!query.isSharedInstance()){
					bean = descriptor.createCopy(bean);
				}
				descriptor.cachePut(bean);
			}
        }

        if (query.isSharedInstance() && !result.isEmpty()){
        	if (query.isUseQueryCache()){
            	// load the query result into the query cache
            	request.putToQueryCache(result);        		
        	}
        	if (Boolean.FALSE.equals(query.isReadOnly())){
        		// create a copy of the collection and beans that is mutable
        		result = new CopyBeanCollection<T>(result, request.getBeanDescriptor()).copy();
        	}
        }
        
        return result;
    }


    /**
     * Find a single bean using its unique id.
     */
	public <T> T findId(OrmQueryRequest<T> request) {
        
		T result = null;
		        
        SpiTransaction t = request.getTransaction();
        
        if (t.isBatchFlushOnQuery()){
            // before we perform a query, we need to flush any
            // previous persist requests that are queued/batched.
            // The query may read data affected by those requests.
        	t.flushBatch();
        }
        
        BeanFinder<T> finder = request.getBeanFinder();
        if (finder != null) {
            result =  finder.find(request);
        } else {
        	result = queryEngine.find(request);
        }
        
        if (result != null && request.isUseBeanCache()){
        	BeanDescriptor<T> descriptor = request.getBeanDescriptor();
        	T cacheBean = result;
        	if (!request.isUseBeanCacheReadOnly()){
        		// put a copy into the cache
        		cacheBean = descriptor.createCopy(result);
        	} 
            descriptor.cachePut(cacheBean);        		
        }
        
        return result;
    }


}
