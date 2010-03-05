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

import java.util.List;

import com.avaje.ebean.bean.BeanLoader;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebeaninternal.api.LoadBeanContext;
import com.avaje.ebeaninternal.api.LoadBeanRequest;
import com.avaje.ebeaninternal.api.LoadContext;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryProperties;

/**
 * Default implementation of LoadBeanContext.
 *
 */
public class DLoadBeanContext implements LoadBeanContext, BeanLoader {

	protected final DLoadContext parent;

	protected final BeanDescriptor<?> desc;
	
	protected final String path;
	protected final String fullPath;

	private final DLoadWeakList<EntityBeanIntercept> weakList;

	private final OrmQueryProperties queryProps;
	
	private int batchSize;

	public DLoadBeanContext(DLoadContext parent, BeanDescriptor<?> desc, String path, int batchSize, OrmQueryProperties queryProps) {
		this.parent = parent;
		this.desc = desc;
		this.path = path;
		this.batchSize = batchSize;
		this.queryProps = queryProps;
		this.weakList = new DLoadWeakList<EntityBeanIntercept>();
		
		if (parent.getRelativePath() == null){
			this.fullPath = path;
		} else {
			this.fullPath = parent.getRelativePath()+"."+path;
		}
	}
	
	public void configureQuery(SpiQuery<?> query, String lazyLoadProperty){
		
		// propagate the sharedInstance/ReadOnly state
		query.setParentState(parent.getParentState());
		query.setParentNode(getObjectGraphNode());
		query.setLazyLoadProperty(lazyLoadProperty);
		
		if (queryProps != null){
			queryProps.configureBeanQuery(query);
		} 
	}

	public String getFullPath() {
		return fullPath;
	}
	
	public PersistenceContext getPersistenceContext() {
		return parent.getPersistenceContext();
	}

	public OrmQueryProperties getQueryProps() {
		return queryProps;
	}

	public ObjectGraphNode getObjectGraphNode() {
		return parent.getObjectGraphNode(path);
	}
	
	public String getPath() {
		return path;
	}
	
	public String getName() {
		return parent.getEbeanServer().getName();
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public BeanDescriptor<?> getBeanDescriptor() {
		return desc;
	}

	public LoadContext getGraphContext() {
		return parent;
	}

	public void register(EntityBeanIntercept ebi){
		int pos = weakList.add(ebi);
		ebi.setBeanLoader(pos, this);
	}

	public void loadBean(EntityBeanIntercept ebi) {
		
		int position = ebi.getBeanLoaderIndex();
		
//		Object id = desc.getId(ebi.getOwner());
//		if (desc.refreshFromCache(ebi, id)) {
//			// we hit the cache... so not even bother with 
//			// querying the DB. Just null out the appropriate
//			// weak reference entry out of the list...
//			EntityBeanIntercept removeEntry = weakList.removeEntry(position);
//			if (removeEntry != ebi){
//				throw new RuntimeException("Different instance returned?");
//			}
//			return;
//		}
		
		// determine the set of beans to lazy load
		List<EntityBeanIntercept> batch = weakList.getLoadBatch(position, batchSize);

		LoadBeanRequest req = new LoadBeanRequest(this, batch, null, batchSize, true, ebi.getLazyLoadProperty());

		parent.getEbeanServer().loadBean(req);
	}
	
	public void load(OrmQueryRequest<?> parentRequest, int requestedBatchSize) {
		
		// determine the set of beans to load
		List<EntityBeanIntercept> batch = weakList.getLoadBatch(0, requestedBatchSize);
		if (batch.size() == 0){
		    // there are no beans to load
		} else {
    		LoadBeanRequest req = new LoadBeanRequest(this, batch, parentRequest.getTransaction(), requestedBatchSize, false, null);
    		parent.getEbeanServer().loadBean(req);
		}
	}
	
}
