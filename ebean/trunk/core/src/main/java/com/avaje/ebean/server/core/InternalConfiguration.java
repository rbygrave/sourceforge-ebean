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

import java.util.logging.Logger;

import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.config.ExternalTransactionManager;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.internal.BackgroundExecutor;
import com.avaje.ebean.internal.SpiEbeanServer;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.autofetch.AutoFetchManagerFactory;
import com.avaje.ebean.server.cache.ServerCacheManager;
import com.avaje.ebean.server.deploy.BeanDescriptorManager;
import com.avaje.ebean.server.deploy.DeployOrmXml;
import com.avaje.ebean.server.deploy.parse.DeployCreateProperties;
import com.avaje.ebean.server.deploy.parse.DeployInherit;
import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.expression.DefaultExpressionFactory;
import com.avaje.ebean.server.jmx.MAdminLogging;
import com.avaje.ebean.server.lib.cluster.ClusterManager;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.persist.DefaultPersister;
import com.avaje.ebean.server.query.CQueryEngine;
import com.avaje.ebean.server.query.DefaultOrmQueryEngine;
import com.avaje.ebean.server.query.DefaultRelationalQueryEngine;
import com.avaje.ebean.server.resource.ResourceManager;
import com.avaje.ebean.server.resource.ResourceManagerFactory;
import com.avaje.ebean.server.subclass.SubClassManager;
import com.avaje.ebean.server.transaction.DefaultTransactionScopeManager;
import com.avaje.ebean.server.transaction.ExternalTransactionScopeManager;
import com.avaje.ebean.server.transaction.TransactionManager;
import com.avaje.ebean.server.transaction.TransactionScopeManager;
import com.avaje.ebean.server.type.DefaultTypeManager;
import com.avaje.ebean.server.type.TypeManager;

/**
 * Used to extend the ServerConfig with additional objects used
 * to configure and construct an EbeanServer.
 * 
 * @author rbygrave
 */
public class InternalConfiguration {

	private static final Logger logger = Logger.getLogger(InternalConfiguration.class.getName());
	
	private final ServerConfig serverConfig;

	private final BootupClasses bootupClasses;

	private final SubClassManager subClassManager;

	private final DeployInherit deployInherit;

	private final ResourceManager resourceManager;

	private final DeployOrmXml deployOrmXml;
	
	private final TypeManager typeManager;
	
	private final Binder binder;
	
	private final DeployCreateProperties deployCreateProperties;
	
	private final DeployUtil deployUtil;
	
	private final BeanDescriptorManager beanDescriptorManager;
	
	private final MAdminLogging logControl;
	
	private final RefreshHelp refreshHelp;
	
	private final DebugLazyLoad debugLazyLoad;
	
	private final TransactionManager transactionManager;
	
	private final TransactionScopeManager transactionScopeManager;
	
	private final CQueryEngine cQueryEngine;
	
	private final ClusterManager clusterManager;
		
	private final ServerCacheManager cacheManager;
	
	private final ExpressionFactory expressionFactory;
	
	private final BackgroundExecutor backgroundExecutor;
	
	public InternalConfiguration(ClusterManager clusterManager, ServerCacheManager cacheManager, 
			BackgroundExecutor backgroundExecutor, ServerConfig serverConfig, BootupClasses bootupClasses) {
		
		this.clusterManager = clusterManager;
		this.backgroundExecutor = backgroundExecutor;
		this.cacheManager = cacheManager;
		this.serverConfig = serverConfig;
		this.bootupClasses = bootupClasses;
		this.expressionFactory = new DefaultExpressionFactory();
		
		this.subClassManager = new SubClassManager(serverConfig);
		
		this.typeManager = new DefaultTypeManager(serverConfig, bootupClasses);
		this.binder = new Binder(typeManager);
		
		this.resourceManager = ResourceManagerFactory.createResourceManager(serverConfig);
		this.deployOrmXml = new DeployOrmXml(resourceManager.getResourceSource());
		this.deployInherit = new DeployInherit(bootupClasses);		
		
		this.deployCreateProperties = new DeployCreateProperties(typeManager);
		this.deployUtil = new DeployUtil(typeManager, serverConfig.getNamingConvention(), serverConfig.getDatabasePlatform());

		
		this.beanDescriptorManager = new BeanDescriptorManager(this);
		beanDescriptorManager.deploy();
		
		this.debugLazyLoad = new DebugLazyLoad(serverConfig.isDebugLazyLoad());
		
		this.transactionManager = new TransactionManager(clusterManager, serverConfig, beanDescriptorManager);

		this.logControl = new MAdminLogging(serverConfig, transactionManager);
		this.cQueryEngine = new CQueryEngine(serverConfig.getDatabasePlatform(), logControl, binder);
		this.refreshHelp = new RefreshHelp(logControl, serverConfig.isDebugLazyLoad());

		
		ExternalTransactionManager externalTransactionManager = serverConfig.getExternalTransactionManager();
		if (externalTransactionManager != null){
			
			externalTransactionManager.setTransactionManager(transactionManager);
			this.transactionScopeManager = new ExternalTransactionScopeManager(transactionManager,externalTransactionManager);
			logger.info("Using external Transaction Manager");
		} else {
			this.transactionScopeManager = new DefaultTransactionScopeManager(transactionManager);			
		}
		
	}

	public AutoFetchManager createAutoFetchManager(SpiEbeanServer server){
		return AutoFetchManagerFactory.create(server, serverConfig, resourceManager);
	}


	public RelationalQueryEngine createRelationalQueryEngine() {
		return new DefaultRelationalQueryEngine(logControl, binder);
	}
	
	public OrmQueryEngine createOrmQueryEngine() {
		return new DefaultOrmQueryEngine(beanDescriptorManager,  cQueryEngine);
	}
	
	public Persister createPersister(SpiEbeanServer server) {
		return new DefaultPersister(server, serverConfig.isValidateOnSave(), logControl, binder, beanDescriptorManager);
	}

	public ServerCacheManager getCacheManager() {
		return cacheManager;
	}

	public BootupClasses getBootupClasses() {
		return bootupClasses;
	}

	public DatabasePlatform getDatabasePlatform() {
		return serverConfig.getDatabasePlatform();
	}
	
	public ServerConfig getServerConfig() {
		return serverConfig;
	}
	
	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public TypeManager getTypeManager() {
		return typeManager;
	}
	
	public Binder getBinder() {
		return binder;
	}

	public BeanDescriptorManager getBeanDescriptorManager() {
		return beanDescriptorManager;
	}

	public SubClassManager getSubClassManager() {
		return subClassManager;
	}

	public DeployInherit getDeployInherit() {
		return deployInherit;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public DeployOrmXml getDeployOrmXml() {
		return deployOrmXml;
	}

	public DeployCreateProperties getDeployCreateProperties() {
		return deployCreateProperties;
	}

	public DeployUtil getDeployUtil() {
		return deployUtil;
	}

	public MAdminLogging getLogControl() {
		return logControl;
	}


	public TransactionManager getTransactionManager() {
		return transactionManager;
	}


	public TransactionScopeManager getTransactionScopeManager() {
		return transactionScopeManager;
	}


	public CQueryEngine getCQueryEngine() {
		return cQueryEngine;
	}


	public ClusterManager getClusterManager() {
		return clusterManager;
	}


	public RefreshHelp getRefreshHelp() {
		return refreshHelp;
	}


	public DebugLazyLoad getDebugLazyLoad() {
		return debugLazyLoad;
	}

	public BackgroundExecutor getBackgroundExecutor() {
		return backgroundExecutor;
	}

}
