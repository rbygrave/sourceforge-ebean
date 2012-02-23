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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.bean.BeanLoader;
import com.avaje.ebean.bean.EntityBean;
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

	private static final Logger logger = Logger.getLogger(DLoadBeanContext.class.getName());
	
	protected final DLoadContext parent;

	protected final BeanDescriptor<?> desc;
	
	protected final String path;
	
	protected final String fullPath;

	private final DLoadList<EntityBeanIntercept> weakList;

	private final OrmQueryProperties queryProps;
	
	private int batchSize;

  public DLoadBeanContext(DLoadContext parent, BeanDescriptor<?> desc, String path, int batchSize, 
      OrmQueryProperties queryProps, DLoadList<EntityBeanIntercept> weakList) {
    
    this.parent = parent;
    this.desc = desc;
    this.path = path;
    this.batchSize = batchSize;
    this.queryProps = queryProps;
    this.weakList = weakList;

    if (parent.getRelativePath() == null) {
      this.fullPath = path;
    } else {
      this.fullPath = parent.getRelativePath() + "." + path;
    }
  }
	
  public void configureQuery(SpiQuery<?> query, String lazyLoadProperty) {

    // propagate the readOnly state
    if (parent.isReadOnly() != null) {
      query.setReadOnly(parent.isReadOnly());
    }
    query.setParentNode(getObjectGraphNode());
    query.setLazyLoadProperty(lazyLoadProperty);

    if (queryProps != null) {
      queryProps.configureBeanQuery(query);
    }
    if (parent.isUseAutofetchManager()) {
      query.setAutofetch(true);
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

    if (desc.lazyLoadMany(ebi)) {
      // lazy load property was a Many
      return;
    }
    synchronized (weakList) {

      int position = ebi.getBeanLoaderIndex();
      boolean hitCache = !parent.isExcludeBeanCache() && desc.isBeanCaching();

      if (hitCache) {
        if (desc.loadFromCache(ebi)) {
          // we loaded the bean from cache
          weakList.removeEntry(position);
          if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Loading path:" + fullPath + " - bean loaded from L2 cache, position[" + position + "]");
          }
          return;
        }
      }

      // determine the set of beans to lazy load
      List<EntityBeanIntercept> batch = null;
      try {
        batch = weakList.getLoadBatch(position, batchSize);
      } catch (IllegalStateException e) {
        logger.log(Level.SEVERE, "type["+desc.getFullName()+"] fullPath[" + fullPath + "] batchSize["+batchSize+"]", e);
      }
      
      if (hitCache && batchSize > 1) {
        // check each of the beans in the batch to see if they are in the cache
        List<EntityBeanIntercept> actualLoadBatch = new ArrayList<EntityBeanIntercept>(batchSize);
        List<EntityBeanIntercept> batchToCheck = batch;
        int skip = 0;
        while (true) {
          // check each bean (not already checked) to see if it is in the cache
          for (int i = skip; i < batchToCheck.size(); i++) {
            if (desc.loadFromCache(batchToCheck.get(i))) {
              if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "Loading path:" + fullPath + " - bean loaded from L2 cache(batch)");
              }
            } else {
              actualLoadBatch.add(batchToCheck.get(i));
            }
          }
          skip = actualLoadBatch.size();
          if (batchToCheck.size() < batchSize) {
            // we have exhausted all the beans that need lazy loading
            break;
          }
          int more = batchSize - actualLoadBatch.size();
          if (more <= 0) {
            break;
          }
          // get some more to check as we loaded some from L2 cache
          batchToCheck = weakList.getNextBatch(more);
        }
        batch = actualLoadBatch;
      }

      if (batch.isEmpty()) {
        // we must have since loaded the bean we missed earlier
        return;
      }

      if (logger.isLoggable(Level.FINE)) {
        for (int i = 0; i < batch.size(); i++) {
          
          EntityBeanIntercept entityBeanIntercept = batch.get(i);
          EntityBean owner = entityBeanIntercept.getOwner();
          Object id = desc.getId(owner);
          
          logger.fine("LoadBean type["+owner.getClass().getName()+"] id["+id+"] batchIndex["+i+"] beanLoaderIndex["+entityBeanIntercept.getBeanLoaderIndex()+"]");
        }
      }
      
      LoadBeanRequest req = new LoadBeanRequest(this, batch, null, batchSize, true, ebi.getLazyLoadProperty(), hitCache);
      parent.getEbeanServer().loadBean(req);
    }
  }
	
  public void loadSecondaryQuery(OrmQueryRequest<?> parentRequest, int requestedBatchSize, boolean all) {

    synchronized (weakList) {
      do {
        List<EntityBeanIntercept> batch = weakList.getNextBatch(requestedBatchSize);
        if (batch.size() == 0) {
          // there are no beans to load
          if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "Loading path:" + fullPath + " - no more beans to load");
          }
          return;
        }
        boolean loadCache = false;
        LoadBeanRequest req = new LoadBeanRequest(this, batch, parentRequest.getTransaction(), requestedBatchSize, false, null, loadCache);

        if (logger.isLoggable(Level.FINEST)) {
          logger.log(Level.FINEST, "Loading path:" + fullPath + " - secondary query batch load [" + batch.size() + "] beans");
        }

        parent.getEbeanServer().loadBean(req);
        if (!all) {
          break;
        }

      } while (true);
    }
  }
	
}
