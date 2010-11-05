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
package com.avaje.ebeaninternal.server.core;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.common.BeanList;
import com.avaje.ebean.common.BeanMap;
import com.avaje.ebean.common.BeanSet;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.CopyContext;

/**
 * Helper to copy collections when using query cache.
 */
public class CopyBeanCollection<T> {

	private final BeanCollection<T> bc;
	private final BeanDescriptor<T> desc;
	
	private final CopyContext ctx;
	private final int maxDepth;
	
	public CopyBeanCollection(BeanCollection<T> bc, BeanDescriptor<T> desc, CopyContext ctx, int maxDepth) {
		this.bc = bc;
		this.desc = desc;
		this.ctx = ctx;
		this.maxDepth = maxDepth;
	}
	
	public BeanCollection<T> copy(){
		if (bc instanceof BeanList<?>){
			return copyList();
			
		} else if (bc instanceof BeanSet<?>){
			return copySet();
			
		} else if (bc instanceof BeanMap<?,?>){
			return copyMap();
			
		} else {
			String msg = "Invalid beanCollection type "+bc.getClass().getName();
			throw new RuntimeException(msg);
		}
	}
	
	private BeanCollection<T> copyList() {
		BeanList<T> newList = new BeanList<T>();
		List<T> actualList = ((BeanList<T>)bc).getActualList();
		
		for (int i = 0; i < actualList.size(); i++) {
			T t = actualList.get(i);
			newList.add(desc.createCopy(t, ctx, maxDepth));
		}
		return newList;
	}
	
	private BeanCollection<T> copySet() {
		BeanSet<T> newSet = new BeanSet<T>();
		Set<T> actualSet = ((BeanSet<T>)bc).getActualSet();
		for (T t : actualSet) {
			newSet.add(desc.createCopy(t, ctx, maxDepth));
		}
		return newSet;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private BeanCollection<T> copyMap() {
		BeanMap<Object,T> newMap = new BeanMap();
		Map<Object,T> actualMap = ((BeanMap<Object,T>)bc).getActualMap();
		Iterator<Map.Entry<Object, T>> iterator = actualMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Object,T> entry = iterator.next();
			newMap.put(entry.getKey(), desc.createCopy(entry.getValue(), ctx, maxDepth));
		}
		return newMap;
	}
}
