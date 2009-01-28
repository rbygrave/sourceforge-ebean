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
package com.avaje.ebean.server.persist.mapbean;

import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.persist.BeanPersister;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.plugin.PluginDbConfig;

/**
 * Handles insert update and delete for MapBeans.
 * <p>
 * Note that MapBeans do not necessarily have all the properties. So for
 * MapBeans the dml is dynamically generated for each request based on the
 * properties in the MapBean. This also takes into account GeneratedProperty
 * properties such as 'Insert Timestamp' 'Update Timestamp' and 'Counter'.
 * </p>
 * 
 */
public class MapBeanPersister implements BeanPersister {

	/**
	 * True if this jdbc driver supports getGeneratedKeys.
	 */
	private final boolean genKeysSupport;

	/**
	 * Binder that includes BooleanConverter support.
	 */
	private final Binder binder;

	public MapBeanPersister(PluginDbConfig dbConfig) {
	
		binder = dbConfig.getBinder();
		genKeysSupport = dbConfig.isSupportsGetGeneratedKeys();
	}

	/**
	 * execute delete using the MapBean in the request.
	 */
	public void delete(PersistRequest request) {
		DeleteMapBean delete = new DeleteMapBean(binder, request);
		delete.execute();
	}

	/**
	 * execute update using the MapBean in the request.
	 */
	public void update(PersistRequest request) {
		UpdateMapBean update = new UpdateMapBean(binder, request);
		update.execute();
	}

	/**
	 * execute insert using the MapBean in the request.
	 */
	public void insert(PersistRequest request) {

		InsertMapBean insert = new InsertMapBean(binder, request, genKeysSupport);
		insert.execute();
	}

}
