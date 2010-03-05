/**
 * Copyright (C) 2009  Robin Bygrave
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
package com.avaje.ebeaninternal.server.deploy;

import java.util.HashSet;
import java.util.Set;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebeaninternal.server.query.SqlTreeNode;

/**
 * Helper for performing a 'refresh' on an Entity bean.
 * <p>
 * Note that this does not 'refresh' any OnetoMany or ManyToMany properties. It
 * refreshes all the other properties though.
 * </p>
 */
public class BeanRefreshFromCacheHelp {

	private final BeanDescriptor<?> desc;
	private final EntityBeanIntercept ebi;
	private final EntityBean bean;
	private final Object cacheBean;
	private final Object originalOldValues;
	private final boolean isLazyLoad;
	private final boolean readOnly;
	private final boolean sharedInstance;
	private final int parentState;

	// set of properties to exclude from the refresh because it is
	// not a refresh but rather a lazyLoading event.
	private final Set<String> excludes;
	private final Set<String> cacheBeanLoadedProps;
	private final Set<String> loadedProps;
	
	private final boolean setOriginalOldValues;
	
	public BeanRefreshFromCacheHelp(BeanDescriptor<?> desc, EntityBeanIntercept ebi, Object cacheBean, boolean isLazyLoad){
		this.desc = desc;
		this.ebi = ebi;
		this.bean = ebi.getOwner();
		this.cacheBean = cacheBean;
		this.cacheBeanLoadedProps = ((EntityBean)cacheBean)._ebean_getIntercept().getLoadedProps();
		
		if (cacheBeanLoadedProps != null){
			loadedProps = new HashSet<String>();
		} else {
			loadedProps = null;
		}
		
		this.isLazyLoad = isLazyLoad;
		this.readOnly = ebi.isReadOnly();
		this.sharedInstance = ebi.isSharedInstance();
		if (sharedInstance){
			parentState = SqlTreeNode.SHARED;
		} else if (readOnly){
			parentState = SqlTreeNode.READONLY;
		} else {
			parentState = SqlTreeNode.NORMAL;
		}
		
		this.excludes = isLazyLoad ? ebi.getLoadedProps() : null;
		if (excludes != null){
			// lazy loading a "Partial Object"... which already
			// contains some properties and perhaps some oldValues
			// and these will need to be maintained...
			originalOldValues = ebi.getOldValues();
		} else {
			originalOldValues = null;
		}
		this.setOriginalOldValues = originalOldValues != null;	
	}

	private boolean includeProperty(BeanProperty prop) {
		String name = prop.getName();
		if (excludes != null && excludes.contains(name)){
			// ignore this property (partial bean lazy loading)
			return false;
		}
		if (cacheBeanLoadedProps != null && !cacheBeanLoadedProps.contains(name)){
			return false;
		}
		if (loadedProps != null){
			loadedProps.add(name);
		}
		return true;
	}
	
	/**
	 * Refresh the bean from property values in dbBean.
	 */
	public void refresh() {

		// turn off intercepting so lazy loading is
		// not invoked when populating the bean
		// with PropertyChangeSupport
		ebi.setIntercepting(false);
				
		BeanProperty[] props = desc.propertiesBaseScalar();
		for (int i = 0; i < props.length; i++) {
			BeanProperty prop = props[i];
			if (includeProperty(prop)){
				Object val = prop.getValue(cacheBean);
				if (isLazyLoad) {
					prop.setValue(bean, val);
				} else {			
					prop.setValueIntercept(bean, val);
				}
				if (setOriginalOldValues){
					// maintain original oldValues for partially loaded bean
					prop.setValue(originalOldValues, val);
				}
			}
		}

		BeanPropertyAssocOne<?>[] ones = desc.propertiesOne();
		for (int i = 0; i < ones.length; i++) {
			BeanPropertyAssocOne<?> prop = ones[i];
			if (includeProperty(prop)){
				// returns a reference from the cache with 'sharedInstance' set
				Object val = prop.getValue(cacheBean);
				if (!sharedInstance){
					// create a copy so that we can change its state...
					val = prop.getTargetDescriptor().createCopy(val);
				}
				if (isLazyLoad){
					prop.setValue(bean, val);					
				} else {
					prop.setValueIntercept(bean, val);
				}
				if (setOriginalOldValues){
					// maintain original oldValues for partially loaded bean
					prop.setValue(originalOldValues, val);
				}
				if (val != null && parentState > 0){
					((EntityBean)val)._ebean_getIntercept().propagateParentState(parentState);
				}
				
			}
		}

		refreshEmbedded();

		// set a lazy loading many proxy if required
		BeanPropertyAssocMany<?>[] manys = desc.propertiesMany();
		for (int i = 0; i < manys.length; i++) {
			BeanPropertyAssocMany<?> prop = manys[i];
			if (includeProperty(prop)){
				// set a lazy loading proxy
				prop.createReference(bean);				
			}
		}
		
		ebi.setLoadedProps(loadedProps);
		
		// reset the loaded status
		ebi.setLoaded();
	}

	/**
	 * Refresh the Embedded beans.
	 */
	private void refreshEmbedded(){

		BeanPropertyAssocOne<?>[] embeds = desc.propertiesEmbedded();
		for (int i = 0; i < embeds.length; i++) {
			BeanPropertyAssocOne<?> prop = embeds[i];
			if (includeProperty(prop)){
				// the original embedded bean
				Object oEmb = prop.getValue(bean);
				
				// the new one from the database
				Object cacheEmb = prop.getValue(cacheBean);
	
				if (oEmb == null){
					// original embedded bean was null
					// so just replace the entire embedded bean
					if (cacheEmb == null){
						prop.setValueIntercept(bean, null);
						
					} else {
						Object copyEmb = prop.getTargetDescriptor().createCopy(cacheEmb);
						prop.setValueIntercept(bean, copyEmb);
						if (copyEmb != null && parentState > 0){
							((EntityBean)copyEmb)._ebean_getIntercept().propagateParentState(parentState);
						}
					}
					
				} else {
					// refresh each property of the original
					// embedded bean
					if (oEmb instanceof EntityBean){
						// turn off interception to stop invoking lazy loading
						// but allow PropertyChangeSupport
						((EntityBean) oEmb)._ebean_getIntercept().setIntercepting(false);
					}
					
					BeanProperty[] props = prop.getProperties();
					for (int j = 0; j < props.length; j++) {
						Object v = props[j].getValue(cacheEmb);
						props[j].setValueIntercept(oEmb, v);
					}
		
					// No longer calling setLoaded() on embedded bean
					// as the EntityBean itself
					// .. calls setEmbeddedLoaded() on each of
					// .. its embedded beans itself.					
				}
			}
		}
	}

}
