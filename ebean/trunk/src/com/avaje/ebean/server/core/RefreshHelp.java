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
package com.avaje.ebean.server.core;

import java.util.Set;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.jmx.MLogControlMBean;
import com.avaje.ebean.server.lib.util.StringHelper;
import com.avaje.ebean.server.plugin.Plugin;

/**
 * Helper for performing a 'refresh' on an Entity bean.
 * <p>
 * Note that this does not 'refresh' any OnetoMany or ManyToMany properties. It
 * refreshes all the other properties though.
 * </p>
 */
public class RefreshHelp {

	/**
	 * Helper for debug of lazy loading.
	 */
	private final DebugLazyLoad debugLazyLoad;
	
	private final MLogControlMBean logControl;
	
	public RefreshHelp(MLogControlMBean logControl, Plugin plugin){
		this.logControl = logControl;
		this.debugLazyLoad = new DebugLazyLoad(plugin);
	}

	/**
	 * Refresh the bean from property values in dbBean.
	 */
	public void refresh(Object o, Object dbBean, BeanDescriptor desc, EntityBeanIntercept ebi, Object id, boolean isLazyLoad) {

		Object originalOldValues = null;
		boolean setOriginalOldValues = false;

		// set of properties to exclude from the refresh because it is
		// not a refresh but rather a lazyLoading event.
		Set<String> excludes = null;
				
		if (ebi != null){
			if (isLazyLoad){
				excludes = ebi.getLoadedProps();
				if (excludes != null){
					// lazy loading a "Partial Object"... which already
					// contains some properties and perhaps some oldValues
					// and these will need to be maintained...
					originalOldValues = ebi.getOldValues();
					setOriginalOldValues = originalOldValues != null;					
				}
				
				if (logControl.isDebugLazyLoad()){
					debug(desc, ebi, id, excludes);
				}				
			}
		}
				
		BeanProperty[] props = desc.propertiesBaseScalar();
		for (int i = 0; i < props.length; i++) {
			BeanProperty prop = props[i];
			if (excludes != null && excludes.contains(prop.getName())){
				// ignore this property (partial bean lazy loading)
				
			} else {
				Object dbVal = prop.getValue(dbBean);
				prop.setValue(o, dbVal);
				
				if (setOriginalOldValues){
					// maintain original oldValues for partially loaded bean
					prop.setValue(originalOldValues, dbVal);
				}
			}
		}

		BeanPropertyAssocOne[] ones = desc.propertiesOne();
		for (int i = 0; i < ones.length; i++) {
			BeanProperty prop = ones[i];
			if (excludes != null && excludes.contains(prop.getName())){
				 // ignore this property (partial bean lazy loading)
				
			} else {
				Object dbVal = prop.getValue(dbBean);
				prop.setValue(o, dbVal);
				
				if (setOriginalOldValues){
					// maintain original oldValues for partially loaded bean
					prop.setValue(originalOldValues, dbVal);
				}
			}
		}

		refreshEmbedded(o, dbBean, desc, excludes);

		BeanPropertyAssocMany[] manys = desc.propertiesMany();
		for (int i = 0; i < manys.length; i++) {
			BeanProperty prop = manys[i];
			if (excludes != null && excludes.contains(prop.getName())){
				 // ignore this property (partial bean lazy loading)
				
			} else {
				Object dbVal = prop.getValue(dbBean);
				prop.setValue(o, dbVal);
			}
		}
		
		if (ebi != null){
			// the refreshed/lazy loaded bean is always fully
			// populated so set loadedProps to null
			ebi.setLoadedProps(null);
			
			if (!isLazyLoad){
				// refresh will reset the loaded status
				ebi.setLoaded();
			}
		}
		
	}

	/**
	 * Refresh the Embedded beans.
	 */
	private void refreshEmbedded(Object o, Object dbBean, BeanDescriptor desc, Set<String> excludes) {

		BeanPropertyAssocOne[] embeds = desc.propertiesEmbedded();
		for (int i = 0; i < embeds.length; i++) {
			BeanPropertyAssocOne prop = embeds[i];
			if (excludes != null && excludes.contains(prop.getName())){
				// ignore this property
			} else {
				// the original embedded bean
				Object oEmb = prop.getValue(o);
				
				// the new one from the database
				Object dbEmb = prop.getValue(dbBean);
	
				if (oEmb == null){
					// original embedded bean was null
					// so just replace the entire embedded bean
					prop.setValue(o, dbEmb);
					
				} else {
					// refresh each property of the original
					// embedded bean
					BeanProperty[] props = prop.getProperties();
					for (int j = 0; j < props.length; j++) {
						Object v = props[j].getValue(dbEmb);
						props[j].setValue(oEmb, v);
					}
		
					if (oEmb instanceof EntityBean) {
						EntityBean eb = (EntityBean) oEmb;
						eb._ebean_getIntercept().setLoaded();
					}
				}
			}
		}
	}
	

	/**
	 * Output some debug to describe the lazy loading event.
	 */
	private void debug(BeanDescriptor desc, EntityBeanIntercept ebi, Object id, Set<String> excludes) {
		
				
		Class<?> beanType = desc.getBeanType();
		
		StackTraceElement cause = debugLazyLoad.getStackTraceElement(beanType);
		
		String lazyLoadProperty = ebi.getLazyLoadProperty();
		String msg = "debug.lazyLoad ["+desc+"] id["+id+"] lazyLoadProperty["+lazyLoadProperty+"]";
		if (excludes != null){
			msg += " partialProps"+excludes;
		} 
		if (cause != null){
			String causeLine = cause.toString();
			if (causeLine.indexOf(".groovy:") > -1){
				// eclipse console does not like finding groovy source at the moment
				causeLine = StringHelper.replaceString(causeLine, ".groovy:", ".groovy :");
			}
			msg += " at: "+causeLine;
		}
		System.err.println(msg);		
	}
	

	

}
