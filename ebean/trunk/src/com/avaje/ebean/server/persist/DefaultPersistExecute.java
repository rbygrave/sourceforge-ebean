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
package com.avaje.ebean.server.persist;

import com.avaje.ebean.bean.BeanController;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.core.PersistRequestCallableSql;
import com.avaje.ebean.server.core.PersistRequestOrmUpdate;
import com.avaje.ebean.server.core.PersistRequestUpdateSql;
import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.jmx.MLogControlMBean;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * Default PersistExecute implementation using DML statements.
 * <p>
 * Supports the use of PreparedStatement batching.
 * </p>
 */
public final class DefaultPersistExecute implements PersistExecute {
  
    /**
     * Defines the logging levels.
     */
    private final MLogControlMBean logControl;
    
    private final ExeCallableSql exeCallableSql;
    
    private final ExeUpdateSql exeUpdateSql;
    
    private final ExeOrmUpdate exeOrmUpdate;
    
	/**
	 * The default batch size.
	 */
	private final int defaultBatchSize;

	/**
	 * Default for whether to call getGeneratedKeys after batch insert.
	 */
	private final boolean defaultBatchGenKeys;
	
    /**
     * Construct this DmlPersistExecute.
     */
    public DefaultPersistExecute(PluginDbConfig dbConfig) {
    
        this.logControl = dbConfig.getLogControl();
        this.exeOrmUpdate = new ExeOrmUpdate(dbConfig);
        this.exeUpdateSql = new ExeUpdateSql(dbConfig);
        this.exeCallableSql = new ExeCallableSql(dbConfig);
        
        PluginProperties properties = dbConfig.getProperties();
		this.defaultBatchGenKeys = properties.getPropertyBoolean("batch.getgeneratedkeys", true);
		this.defaultBatchSize = properties.getPropertyInt("batch.size", 20);
    }

	public BatchControl createBatchControl(ServerTransaction t) {

		// create a BatchControl and set its defaults
		return new BatchControl(t, defaultBatchSize, defaultBatchGenKeys);
	}
	
    /**
     * execute the bean insert request.
     */
    public void executeInsertBean(PersistRequest request) {
    	
    	request.setLogLevel(logControl.getInsertLevel());
    	
    	BeanManager mgr = request.getBeanManager();
    	BeanPersister persister = mgr.getBeanPersister();
    	
    	BeanController controller = request.getBeanController();
		if (controller == null || controller.preInsert(request)) {

			persister.insert(request);
			// NOTE: the persister fires the postInsert so that this 
			// occurs before ebeanIntercept.setLoaded(true)
		} 
    }
    
    /**
     * execute the bean update request.
     */
    public void executeUpdateBean(PersistRequest request) {
    	
    	request.setLogLevel(logControl.getUpdateLevel());

    	BeanManager mgr = request.getBeanManager();
    	BeanPersister persister = mgr.getBeanPersister();
    	
    	BeanController controller = request.getBeanController();
		if (controller == null || controller.preUpdate(request)) {
			
			persister.update(request);
			// NOTE: the persister fires the postUpdate so that this 
			// occurs before ebeanIntercept.setLoaded(true)
		} 
    }

    
    /**
     * execute the bean delete request.
     */
    public void executeDeleteBean(PersistRequest request) {

    	request.setLogLevel(logControl.getDeleteLevel());

    	BeanManager mgr = request.getBeanManager();
    	BeanPersister persister = mgr.getBeanPersister();
    	
    	BeanController controller = request.getBeanController();
		if (controller == null || controller.preDelete(request)) {
			
			persister.delete(request);
			// NOTE: the persister fires the postDelete 
		} 
    }

    /**
     * Execute the updateSqlRequest 
	 */
    public int executeOrmUpdate(PersistRequestOrmUpdate request) {
    	request.setLogLevel(logControl.getOrmUpdateLevel());
    	return exeOrmUpdate.execute(request);
    }
    
    /**
     * Execute the updateSqlRequest 
	 */
    public int executeSqlUpdate(PersistRequestUpdateSql request) {
    	request.setLogLevel(logControl.getSqlUpdateLevel());
    	return exeUpdateSql.execute(request);
    }
    
    /**
     * Execute the CallableSqlRequest.
	 */
    public int executeSqlCallable(PersistRequestCallableSql request) {
    	request.setLogLevel(logControl.getCallableSqlLevel());
    	return exeCallableSql.execute(request);
    }

}
