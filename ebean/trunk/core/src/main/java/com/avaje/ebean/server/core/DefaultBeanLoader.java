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
package com.avaje.ebean.server.core;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.Transaction;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.internal.LoadBeanContext;
import com.avaje.ebean.internal.LoadManyContext;
import com.avaje.ebean.internal.LoadBeanRequest;
import com.avaje.ebean.internal.LoadManyRequest;
import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.internal.SpiQuery.Mode;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.transaction.DefaultPersistenceContext;

public class DefaultBeanLoader {

	private final DebugLazyLoad debugLazyLoad;
	
	private final DefaultServer server;
	
	protected DefaultBeanLoader(DefaultServer server, DebugLazyLoad debugLazyLoad){
		this.server = server;
		this.debugLazyLoad = debugLazyLoad;
	}

	/**
	 * Return a batch size that might be less than the requestedBatchSize.
	 * <p>
	 * This means we can have large and variable requestedBatchSizes.
	 * </p>
	 */
	private int getBatchSize(int batchListSize, int requestedBatchSize) {
		if (batchListSize == requestedBatchSize){
			return batchListSize;
		}
		if (batchListSize == 1) {
			return 1;
		}
		if (requestedBatchSize == 5 && batchListSize < 5){
			return 5;
		}
		if (requestedBatchSize == 10 && batchListSize < 10){
			return 10;
		}
		if (batchListSize < 20){
			return 20;
		}
		if (batchListSize < 50){
			return 50;
		}
		return requestedBatchSize;
	}
	
	public void refreshMany(Object parentBean, String propertyName) {
		refreshMany(parentBean, propertyName, null);
	}

	public void loadMany(LoadManyRequest loadRequest) {
		
	
		List<BeanCollection<?>> batch = loadRequest.getBatch();
		
		int batchSize = getBatchSize(batch.size(), loadRequest.getBatchSize());
		
		LoadManyContext ctx = loadRequest.getLoadContext();
		BeanPropertyAssocMany<?> many = ctx.getBeanProperty();
				
		PersistenceContext pc = ctx.getPersistenceContext();
		
		ArrayList<Object> idList = new ArrayList<Object>(batchSize);
		
		for (int i = 0; i < batch.size(); i++) {
			BeanCollection<?> bc = batch.get(i);
			Object ownerBean = bc.getOwnerBean();
			Object id = many.getParentId(ownerBean);
			idList.add(id);
		}
		int extraIds = batchSize - batch.size();
		if (extraIds > 0){
			Object firstId = idList.get(0);
			for (int i = 0; i < extraIds; i++) {
				idList.add(firstId);
			}
		}
		
		BeanDescriptor<?> desc = ctx.getBeanDescriptor();
		
		String idProperty = desc.getIdBinder().getIdProperty();

		SpiQuery<?> query = (SpiQuery<?>)server.createQuery(desc.getBeanType());
		query.setMode(Mode.LAZYLOAD_MANY);
		query.setPersistenceContext(pc);
		query.select(idProperty);
		query.join(many.getName());
		if (idList.size() == 1){
			query.where().idEq(idList.get(0));
		} else {
			query.where().idIn(idList);			
		}
		
		
		String mode = loadRequest.isLazy() ? "+lazy" : "+query";
		query.setLoadDescription(mode, loadRequest.getDescription());
		
		ctx.configureQuery(query);
		
		List<?> list = server.findList(query, loadRequest.getTransaction());
		
		if (list.size() != batch.size()) {
			String msg = "Batch lazy loading on Many returned incorrect row count?";
			msg += " "+list.size() +" <> "+batch.size();
			throw new RuntimeException();
		}
	}
	
	public void loadMany(BeanCollection<?> bc, LoadManyContext ctx) {
		
		Object parentBean = bc.getOwnerBean();
		String propertyName = bc.getPropertyName();

		ObjectGraphNode node =  ctx == null ? null : ctx.getObjectGraphNode();
	
		loadManyInternal(parentBean, propertyName, null, false, node);

		if (server.getAdminLogging().isDebugLazyLoad()) {

			Class<?> cls = parentBean.getClass();
			BeanDescriptor<?> desc = server.getBeanDescriptor(cls);
			BeanPropertyAssocMany<?> many = (BeanPropertyAssocMany<?>) desc.getBeanProperty(propertyName);

			StackTraceElement cause = debugLazyLoad.getStackTraceElement(cls);

			String msg = "debug.lazyLoad " + many.getManyType() + " [" + desc + "][" + propertyName + "]";
			if (cause != null) {
				msg += " at: " + cause;
			}
			System.err.println(msg);
		}
	}

	public void refreshMany(Object parentBean, String propertyName, Transaction t) {
		loadManyInternal(parentBean, propertyName, t, true, null);
	}

	private void loadManyInternal(Object parentBean, String propertyName, Transaction t, boolean refresh, ObjectGraphNode node) {

		if (parentBean instanceof EntityBean == false) {
			throw new PersistenceException("Can only refresh a previously queried bean");			
		}

		EntityBean parent = (EntityBean) parentBean;
		EntityBeanIntercept ebi = parent._ebean_getIntercept();
		Class<?> cls = parent.getClass();
		
		BeanDescriptor<?> parentDesc = server.getBeanDescriptor(cls);
		BeanPropertyAssocMany<?> many = (BeanPropertyAssocMany<?>) parentDesc.getBeanProperty(propertyName);

		if (refresh){
			BeanCollection<?> emptyCollection = many.createEmpty();
			many.setValue(parent, emptyCollection);
			if (ebi.isSharedInstance()){
				emptyCollection.setSharedInstance();
			}
		}
		
		PersistenceContext pc = ebi.getPersistenceContext();
		if (pc == null){
			pc = new DefaultPersistenceContext();
			Object parentId = parentDesc.getId(parent);
			pc.put(parentId, parent);
		}

		SpiQuery<?> query = (SpiQuery<?>)server.createQuery(parentDesc.getBeanType());
		
		if (node != null) {
			// so we can hook back to the root query
			query.setParentNode(node);
		}
		
		if (ebi.isSharedInstance()){
			// lazy loading for a sharedInstance (bean in the cache)
			query.setSharedInstance();
		}

		Object parentId = parentDesc.getId(parentBean);
		String idProperty = parentDesc.getIdBinder().getIdProperty();

		query.select(idProperty);
		query.join(many.getName());
		query.where().idEq(parentId);
		query.setMode(Mode.LAZYLOAD_MANY);
		query.setPersistenceContext(pc);
		
		if (ebi.isSharedInstance()){
			query.setSharedInstance();
		} else if (ebi.isReadOnly()){
			query.setReadOnly(true);
		}
		

		server.findUnique(query, t);
	}


	/**
	 * Load a batch of beans for +query or +lazy loading.
	 */
	public void loadBean(LoadBeanRequest loadRequest) {
		
		List<EntityBeanIntercept> batch = loadRequest.getBatch();
			
		if (batch.size() == 0){
			throw new RuntimeException("Nothing in batch?");
		}
		
	
		int batchSize = getBatchSize(batch.size(), loadRequest.getBatchSize());
	
		LoadBeanContext ctx = loadRequest.getLoadContext();
		BeanDescriptor<?> desc = ctx.getBeanDescriptor();
		
		Class<?> beanType = desc.getBeanType();
		
		EntityBeanIntercept[] ebis = batch.toArray(new EntityBeanIntercept[batch.size()]);
		ArrayList<Object> idList = new ArrayList<Object>(batchSize);
		
		for (int i = 0; i < batch.size(); i++) {
			Object bean = batch.get(i).getOwner();
			Object id = desc.getId(bean);
			idList.add(id);
		}
		int extraIds = batchSize - batch.size();
		if (extraIds > 0){
			// for performance make up the Id's to the batch size
			// so we get the same query (for Ebean and the db)
			Object firstId = idList.get(0);
			for (int i = 0; i < extraIds; i++) {
				// just add the first Id again
				idList.add(firstId);
			}
		}
		
		PersistenceContext persistenceContext = ctx.getPersistenceContext();
		
		// query the database 
		for (int i = 0; i < ebis.length; i++) {
			Object parentBean = ebis[i].getParentBean();
			if (parentBean != null) {
				// Special case for OneToOne 
				BeanDescriptor<?> parentDesc = server.getBeanDescriptor(parentBean.getClass());
				Object parentId = parentDesc.getId(parentBean);
				persistenceContext.put(parentId, parentBean);
			}
		}
	
		SpiQuery<?> query = (SpiQuery<?>) server.createQuery(beanType);
		
		query.setMode(Mode.LAZYLOAD_BEAN);
		query.setPersistenceContext(persistenceContext);
		
		String mode = loadRequest.isLazy() ? "+lazy" : "+query";
		query.setLoadDescription(mode, loadRequest.getDescription());
		
		ctx.configureQuery(query, loadRequest.getLazyLoadProperty());
		
		// make sure the query doesn't use the cache
		query.setUseCache(false);
		if (idList.size() == 1){
			query.where().idEq(idList.get(0));
		} else {
			query.where().idIn(idList);
		}
		
		List<?> list = server.findList(query, loadRequest.getTransaction());
		
		if (desc.calculateUseCache(null)){
			for (int i = 0; i < list.size(); i++) {
				Object loadedBean = list.get(i);
				// put a copy into the cache
	        	Object cacheBean = desc.createCopy(loadedBean);
	            desc.cachePutObject(cacheBean);	
			}
		}
	}

	public void refresh(Object bean) {
		EntityBean eb = (EntityBean)bean;
		refreshBeanInternal(eb._ebean_getIntercept(), SpiQuery.Mode.REFRESH_BEAN);
	}
	
	public void loadBean(EntityBeanIntercept ebi) {
		refreshBeanInternal(ebi, SpiQuery.Mode.LAZYLOAD_BEAN);
	}

	private void refreshBeanInternal(EntityBeanIntercept ebi, SpiQuery.Mode mode) {

		NodeUsageCollector collector = null;
		
		EntityBean eb = ebi.getOwner();
		BeanDescriptor<?> desc = server.getBeanDescriptor(eb.getClass());
		Object id = desc.getId(eb);

		if (desc.refreshFromCache(ebi, id)) {
			return;
		}
		
		PersistenceContext pc = ebi.getPersistenceContext();
		if (pc == null){
			// a reference with no existing persistenceContext
			pc = new DefaultPersistenceContext();
			ebi.setPersistenceContext(pc);
			
			pc.put(id, eb);
		}
		
		// query the database 
		SpiQuery<?> query = (SpiQuery<?>) server.createQuery(desc.getBeanType());
		
		// don't collect autoFetch usage profiling information
		// as we just copy the data out of these fetched beans
		// and put the data into the original bean
		query.setUsageProfiling(false);

		Object parentBean = ebi.getParentBean();
		if (parentBean != null) {
			// Special case for OneToOne 
			BeanDescriptor<?> parentDesc = server.getBeanDescriptor(parentBean.getClass());
			Object parentId = parentDesc.getId(parentBean);
			pc.putIfAbsent(parentId, parentBean);
		}
		
		query.setPersistenceContext(pc);

		if (collector != null) {
			query.setParentNode(collector.getNode());
		}

		// make sure the query doesn't use the cache and
		// use readOnly in case we put the bean in the cache
		query.setMode(mode);
		query.setId(id);
		query.setUseCache(false);
		if (ebi.isSharedInstance()){
			query.setSharedInstance();
		} else if (ebi.isReadOnly()){
			query.setReadOnly(true);
		}
		
		Object dbBean = query.findUnique();
		
		if (dbBean == null) {
			String msg = "Bean not found during lazy load or refresh." + " id[" + id + "] type[" + desc.getBeanType() + "]";
			throw new PersistenceException(msg);
		}
		if (desc.calculateUseCache(null)){
			// put a copy into the cache
        	Object cacheBean = desc.createCopy(dbBean);
            desc.cachePutObject(cacheBean);	
		}
	}
}
