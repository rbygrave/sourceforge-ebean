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
package com.avaje.ebean.server.loadcontext;

import java.util.List;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.BeanCollectionLoader;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.internal.LoadManyContext;
import com.avaje.ebean.internal.LoadManyRequest;
import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.querydefn.OrmQueryProperties;

public class DLoadManyContext implements LoadManyContext, BeanCollectionLoader {

	protected final DLoadContext parent;

	protected final String fullPath;

	private final BeanDescriptor<?> desc;
	
	private final BeanPropertyAssocMany<?> property;
	
	private final String path;

	private final int batchSize;

	private final OrmQueryProperties queryProps;
	
	private final DLoadWeakList<BeanCollection<?>> weakList;
	
	public DLoadManyContext(DLoadContext parent, BeanPropertyAssocMany<?> p, 
			String path, int batchSize, OrmQueryProperties queryProps) {
		
		this.parent = parent;
		this.property = p;
		this.desc = p.getBeanDescriptor();
		this.path = path;
		this.batchSize = batchSize;
		this.queryProps = queryProps;
		this.weakList = new DLoadWeakList<BeanCollection<?>>();

		if (parent.getRelativePath() == null){
			this.fullPath = path;
		} else {
			this.fullPath = parent.getRelativePath()+"."+path;
		}

	}
	
	public void configureQuery(SpiQuery<?> query){
		
		// propagate the sharedInstance/ReadOnly state
		query.setParentState(parent.getParentState());
		query.setParentNode(getObjectGraphNode());
		
		if (queryProps != null){
			queryProps.configureManyQuery(query);
		}
				
		if (parent.isUseAutofetchManager()){
			query.setAutofetch(true);
		}
	}

	public ObjectGraphNode getObjectGraphNode() {
		
		// we return the parent node ... as we actually
		// query on the parent selecting just it's id
		
		int pos = path.lastIndexOf('.');
		if (pos == -1){
			return parent.getObjectGraphNode(null);			
		} else {
			String parentPath = path.substring(0, pos);
			return parent.getObjectGraphNode(parentPath);
		}
	}

	public String getFullPath() {
		return fullPath;
	}

	public PersistenceContext getPersistenceContext() {
		return parent.getPersistenceContext();
	}

	public int getBatchSize() {
		return batchSize;
	}

	public BeanPropertyAssocMany<?> getBeanProperty() {
		return property;
	}

	public BeanDescriptor<?> getBeanDescriptor() {
		return desc;
	}

	public String getPath() {
		return path;
	}
	
	public String getName() {
		return parent.getEbeanServer().getName();
	}

	public void register(BeanCollection<?> bc){
		int pos = weakList.add(bc);
		bc.setLoader(pos, this);
	}

	public void loadMany(BeanCollection<?> bc) {
				
		int position = bc.getLoaderIndex();
			
		List<BeanCollection<?>> loadBatch = weakList.getLoadBatch(position, batchSize);
				
		LoadManyRequest req = new LoadManyRequest(this, loadBatch, null, batchSize, true);
		
		parent.getEbeanServer().loadMany(req);
	}
	
	public void load(OrmQueryRequest<?> parentRequest, int requestedBatchSize){

		List<BeanCollection<?>> loadBatch = weakList.getLoadBatch(0, requestedBatchSize);
		
		
		LoadManyRequest req = new LoadManyRequest(this, loadBatch, parentRequest.getTransaction(), requestedBatchSize, false);
		
		parent.getEbeanServer().loadMany(req);	
	}

}
