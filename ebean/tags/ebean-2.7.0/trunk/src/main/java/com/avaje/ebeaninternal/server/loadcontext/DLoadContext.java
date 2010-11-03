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
package com.avaje.ebeaninternal.server.loadcontext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.ObjectGraphOrigin;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebeaninternal.api.LoadContext;
import com.avaje.ebeaninternal.api.LoadSecondaryQuery;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssoc;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryProperties;

/**
 * Default implementation of LoadContext.
 * 
 * @author rbygrave
 */
public class DLoadContext implements LoadContext {

	private final SpiEbeanServer ebeanServer;
	
	private final BeanDescriptor<?> rootDescriptor;
	
	private final Map<String, DLoadBeanContext> beanMap = new HashMap<String, DLoadBeanContext>();
	private final Map<String, DLoadManyContext> manyMap = new HashMap<String, DLoadManyContext>();
	
	private final DLoadBeanContext rootBeanContext;
	
	private final int parentState;
	
	private final int defaultBatchSize;

	private PersistenceContext persistenceContext;
	
	/**
	 * The path relative to the root of the object graph.
	 */
	private String relativePath;
	
	private ObjectGraphOrigin origin;
	
	private Map<String,ObjectGraphNode> nodePathMap = new HashMap<String, ObjectGraphNode>();
	
	private boolean useAutofetchManager;
	
	private List<OrmQueryProperties> secQuery;

	public DLoadContext(SpiEbeanServer ebeanServer, BeanDescriptor<?> rootDescriptor, int defaultBatchSize, int parentState, SpiQuery<?> query) {
		
		this.defaultBatchSize = defaultBatchSize;
		this.ebeanServer = ebeanServer;
		this.rootDescriptor = rootDescriptor;
		this.rootBeanContext = new DLoadBeanContext(this, rootDescriptor, null, defaultBatchSize, null);
		this.parentState = parentState;
		
		ObjectGraphNode node = query.getParentNode();
		if (node != null){
			this.origin = node.getOriginQueryPoint();
			this.relativePath = node.getPath();			
		}
		
		useAutofetchManager = query.getAutoFetchManager() != null;		
	}	

	/**
	 * Return the minimum batch size when using QueryIterator with query joins.
	 */
    public int getSecondaryQueriesMinBatchSize(OrmQueryRequest<?> parentRequest, int defaultQueryBatch) {

        if (secQuery == null){
            return -1;
        }
        
        int maxBatch = 0;
        for (int i = 0; i < secQuery.size(); i++) {
            int batchSize = secQuery.get(i).getQueryFetchBatch();
            if (batchSize == 0){
                batchSize = defaultQueryBatch;
            }
            maxBatch = Math.max(maxBatch, batchSize);
        }
        return maxBatch;
    }
    
	/**
	 * Execute all the secondary queries.
	 */
	public void executeSecondaryQueries(OrmQueryRequest<?> parentRequest, int defaultQueryBatch) {
		
		if (secQuery != null){
			for (int i = 0; i < secQuery.size(); i++) {
				OrmQueryProperties properties = secQuery.get(i);
				
				int batchSize = properties.getQueryFetchBatch();
				if (batchSize == 0){
					batchSize = defaultQueryBatch;
				}
				LoadSecondaryQuery load = getLoadSecondaryQuery(properties.getPath());
				load.loadSecondaryQuery(parentRequest, batchSize, properties.isQueryFetchAll());
			}
		}
	}
	
	/**
	 * Return the LoadBeanContext or LoadManyContext for the given path.
	 */
	private LoadSecondaryQuery getLoadSecondaryQuery(String path){
		LoadSecondaryQuery beanLoad = beanMap.get(path);
		if (beanLoad == null){
			beanLoad = manyMap.get(path);
		}
		return beanLoad;
	}

	/**
	 * Remove the +query and +lazy secondary queries and
	 * register them with their appropriate LoadBeanContext
	 * or LoadManyContext. 
	 * <p>
	 * The parts of the secondary queries are removed and used 
	 * by LoadBeanContext/LoadManyContext to build the appropriate
	 * queries. 
	 * </p>
	 */
	public void registerSecondaryQueries(SpiQuery<?> query) {
	
		secQuery = query.removeQueryJoins();
		if (secQuery != null){
			for (int i = 0; i < secQuery.size(); i++) {
				OrmQueryProperties props = secQuery.get(i);
				registerSecondaryQuery(props);
			}
		}
		
		List<OrmQueryProperties> lazyQueries = query.removeLazyJoins();
		if (lazyQueries != null){
			for (int i = 0; i < lazyQueries.size(); i++) {
				OrmQueryProperties lazyProps = lazyQueries.get(i);
				registerSecondaryQuery(lazyProps);
			}
		}
	}
	
	/**
	 * Setup the load context at this path with OrmQueryProperties which is
	 * used to build the appropriate query for +query or +lazy loading.
	 */
	private void registerSecondaryQuery(OrmQueryProperties props) {
					
		String propName = props.getPath();
		ElPropertyValue elGetValue = rootDescriptor.getElGetValue(propName);

		boolean many = elGetValue.getBeanProperty().containsMany();
		registerSecondaryNode(many, props);
	}


	public ObjectGraphNode getObjectGraphNode(String path) {
	
		ObjectGraphNode node = nodePathMap.get(path);
		if (node == null){
			node = createObjectGraphNode(path);
			nodePathMap.put(path, node);
		}
		
		return node;
	}
	
	private ObjectGraphNode createObjectGraphNode(String path) {
		
		if (relativePath != null){
			if (path == null){
				path = relativePath;
			} else {
				path = relativePath+"."+path;
			}
		}
		return new ObjectGraphNode(origin, path);		
	}

	public boolean isUseAutofetchManager() {
		return useAutofetchManager;
	}
	
	public String getRelativePath() {
		return relativePath;
	}

	protected SpiEbeanServer getEbeanServer() {
		return ebeanServer;
	}
	
	public int getParentState() {
		return parentState;
	}
	
	public PersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

	public void setPersistenceContext(PersistenceContext persistenceContext) {
		this.persistenceContext = persistenceContext;
	}

	public void register(String path, EntityBeanIntercept ebi){
		getBeanContext(path).register(ebi);
	}

	public void register(String path, BeanCollection<?> bc){
		getManyContext(path).register(bc);
	}
	
	public DLoadBeanContext getBeanContext(String path) {
		if (path == null){
			return rootBeanContext;
		}
		DLoadBeanContext beanContext = beanMap.get(path);
		if (beanContext == null){
			beanContext = createBeanContext(path, defaultBatchSize, null);
			beanMap.put(path, beanContext);
		}
		return beanContext;
	}
	
	private void registerSecondaryNode(boolean many, OrmQueryProperties props) {
		
		String path = props.getPath();
		int lazyJoinBatch = props.getLazyFetchBatch();
		int batchSize = lazyJoinBatch > 0 ? lazyJoinBatch : defaultBatchSize;
		
		if (many){
			DLoadManyContext manyContext = createManyContext(path, batchSize, props);
			manyMap.put(path, manyContext);
		} else {
			DLoadBeanContext beanContext = createBeanContext(path, batchSize, props);
			beanMap.put(path, beanContext);
		}
	}

	public DLoadManyContext getManyContext(String path) {
		if (path == null){
			throw new RuntimeException("path is null?");
		}
		DLoadManyContext ctx = manyMap.get(path);
		if (ctx == null){
			ctx = createManyContext(path, defaultBatchSize, null);
			manyMap.put(path, ctx);
		}
		return ctx;
	}
	
	private DLoadManyContext createManyContext(String path, int batchSize, OrmQueryProperties queryProps) {

		BeanPropertyAssocMany<?> p = (BeanPropertyAssocMany<?>)getBeanProperty(rootDescriptor, path);

		return new DLoadManyContext(this, p, path, batchSize, queryProps);
	}

	
	private DLoadBeanContext createBeanContext(String path, int batchSize, OrmQueryProperties queryProps) {

		BeanPropertyAssoc<?> p = (BeanPropertyAssoc<?>)getBeanProperty(rootDescriptor, path);
		BeanDescriptor<?> targetDescriptor = p.getTargetDescriptor();

		return new DLoadBeanContext(this, targetDescriptor, path, batchSize, queryProps);			
	}
	
	private BeanProperty getBeanProperty(BeanDescriptor<?> desc, String path){
		
		return desc.getBeanPropertyFromPath(path);
	}
	
}
