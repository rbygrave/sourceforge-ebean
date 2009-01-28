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
package org.avaje.ebean.server.deploy;

import org.avaje.ebean.server.deploy.jointree.JoinTree;
import org.avaje.ebean.server.deploy.jointree.JoinTreeFactory;
import org.avaje.lib.util.FactoryHelper;
import org.avaje.ebean.server.persist.BeanPersister;
import org.avaje.ebean.server.persist.BeanPersisterFactory;
import org.avaje.ebean.server.persist.dml.DmlBeanPersisterFactory;
import org.avaje.ebean.server.plugin.PluginDbConfig;

/**
 * Creates BeanManagers.
 */
public class BeanManagerFactory {

	final BeanPersisterFactory peristerFactory;
	
	final JoinTreeFactory joinTreeFactory;
	
	public BeanManagerFactory(PluginDbConfig dbConfig) {
		joinTreeFactory = new JoinTreeFactory(dbConfig);
		peristerFactory = createBeanPersisterFactory(dbConfig);
	}
	
	public BeanManager create(BeanDescriptor desc) {

		if (desc.isBaseTableNotFound()){
			return new BeanManager(desc, null, null);
		}
		
		BeanPersister persister = peristerFactory.create(desc);
		JoinTree joinTree = joinTreeFactory.create(desc);
		
		return new BeanManager(desc, joinTree, persister);
	}

    private BeanPersisterFactory createBeanPersisterFactory(PluginDbConfig dbConfig) {
    	
    	String cn = dbConfig.getProperties().getProperty("persisterFactory", null);
    	if (cn != null){
    		Class<?>[] argTypes = {PluginDbConfig.class};
    		Object[] args = {dbConfig};
    		
    		return (BeanPersisterFactory)FactoryHelper.create(cn, argTypes, args);
    		
    	} else {
    		return new DmlBeanPersisterFactory(dbConfig);
    	}
    }
}
