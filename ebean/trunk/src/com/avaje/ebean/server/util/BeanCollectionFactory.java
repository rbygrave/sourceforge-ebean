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
package com.avaje.ebean.server.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.collection.BeanList;
import com.avaje.ebean.collection.BeanMap;
import com.avaje.ebean.collection.BeanSet;
import com.avaje.ebean.server.deploy.ManyType;

/**
 * Creates the BeanCollections.
 * <p>
 * Creates the BeanSet BeanMap and BeanList objects.
 * </p>
 */
public class BeanCollectionFactory {

	private static class BeanCollectionFactoryHolder {
		private static BeanCollectionFactory me = new BeanCollectionFactory();
	}
	
    private final int defaultListInitialCapacity = 20;
    private final int defaultSetInitialCapacity = 32;
    private final int defaultMapInitialCapacity = 32;

    private BeanCollectionFactory() {

    }

    /**
     * Create a BeanCollection for the given parameters.
     */
    public static BeanCollection<?> create(BeanCollectionParams params) {
        return BeanCollectionFactoryHolder.me.createMany(params);
    }

    
    private BeanCollection<?> createMany(BeanCollectionParams params) {

        ManyType manyType = params.getManyType();
        
        if (manyType.isMap()) {
            return createMap(params);
        }
        if (manyType.isList()) {
            return createList(params);
        }
        if (manyType.isSet()) {
            return createSet(params);
        }
        throw new RuntimeException("Invalid Arg " + manyType);
    }

    @SuppressWarnings("unchecked")
	private BeanMap createMap(BeanCollectionParams params) {
        
        Boolean ordered = params.getOrdered();

        Map m = null;
        if (ordered == null || ordered) {
            m = new LinkedHashMap(defaultMapInitialCapacity);
        } else {
            m = new HashMap(defaultMapInitialCapacity);
        }

        return new BeanMap(m);       
    }

    @SuppressWarnings("unchecked")
	private BeanSet createSet(BeanCollectionParams params) {
        Boolean ordered = params.getOrdered();
      
        Set s = null;
        if (ordered == null || ordered) {
            s = new LinkedHashSet(defaultSetInitialCapacity);

        } else {
            s = new HashSet(defaultSetInitialCapacity);
        }

        return new BeanSet(s);
    }

    @SuppressWarnings("unchecked")
	private BeanList createList(BeanCollectionParams params) {
        
        List l = new ArrayList(defaultListInitialCapacity);

        return new BeanList(l);
    }
}
