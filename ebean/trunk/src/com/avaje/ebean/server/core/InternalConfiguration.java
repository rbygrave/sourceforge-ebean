package com.avaje.ebean.server.core;

import java.util.logging.Logger;

import com.avaje.ebean.config.ExternalTransactionManager;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.enhance.subclass.SubClassManager;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.autofetch.AutoFetchManagerFactory;
import com.avaje.ebean.server.cache.CacheManager;
import com.avaje.ebean.server.deploy.BeanDescriptorManager;
import com.avaje.ebean.server.deploy.DeployOrmXml;
import com.avaje.ebean.server.deploy.DeploySqlSelectParser;
import com.avaje.ebean.server.deploy.parse.DeployCreateProperties;
import com.avaje.ebean.server.deploy.parse.DeployInherit;
import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.jmx.MLogControl;
import com.avaje.ebean.server.lib.cluster.ClusterManager;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.persist.DefaultPersister;
import com.avaje.ebean.server.query.CQueryEngine;
import com.avaje.ebean.server.query.DefaultOrmQueryEngine;
import com.avaje.ebean.server.query.DefaultRelationalQueryEngine;
import com.avaje.ebean.server.resource.ResourceManager;
import com.avaje.ebean.server.resource.ResourceManagerFactory;
import com.avaje.ebean.server.transaction.DefaultTransactionScopeManager;
import com.avaje.ebean.server.transaction.ExternalTransactionScopeManager;
import com.avaje.ebean.server.transaction.TransactionManager;
import com.avaje.ebean.server.transaction.TransactionScopeManager;
import com.avaje.ebean.server.type.DefaultTypeManager;
import com.avaje.ebean.server.type.TypeManager;

public class InternalConfiguration {

	private static final Logger logger = Logger.getLogger(InternalConfiguration.class.getName());
	
	final ServerConfig serverConfig;

	final BootupClasses bootupClasses;

	final SubClassManager subClassManager;

	final DeployInherit deployInherit;

	final ResourceManager resourceManager;

	final DeployOrmXml deployOrmXml;
	
	final TypeManager typeManager;
	
	final Binder binder;
	
	final DeployCreateProperties deployCreateProperties;
	
	final DeploySqlSelectParser deploySqlSelectParser;
	
	final DeployUtil deployUtil;
	
	final BeanDescriptorManager beanDescriptorManager;
	
	final MLogControl logControl;
	
	final RefreshHelp refreshHelp;
	final DebugLazyLoad debugLazyLoad;
	
	final TransactionManager transactionManager;
	final TransactionScopeManager transactionScopeManager;
	
	final CQueryEngine cQueryEngine;
	
	final ClusterManager clusterManager;
		
	public InternalConfiguration(ClusterManager clusterManager, ServerConfig serverConfig, BootupClasses bootupClasses) {
		
		this.clusterManager = clusterManager;
		this.serverConfig = serverConfig;
		this.bootupClasses = bootupClasses;
		
		this.subClassManager = new SubClassManager(serverConfig);
		
		this.typeManager = new DefaultTypeManager(serverConfig, bootupClasses);
		this.binder = new Binder(typeManager);
		
		this.resourceManager = ResourceManagerFactory.createResourceManager(serverConfig);
		this.deployOrmXml = new DeployOrmXml(resourceManager.getResourceSource());
		this.deployInherit = new DeployInherit(bootupClasses);		
		
		this.deployCreateProperties = new DeployCreateProperties(typeManager);
		this.deploySqlSelectParser = new DeploySqlSelectParser(serverConfig.getNamingConvention());
		this.deployUtil = new DeployUtil(serverConfig.getDatabasePlatform(), typeManager, serverConfig.getNamingConvention());

		
		this.beanDescriptorManager = new BeanDescriptorManager(this);
		beanDescriptorManager.deploy();
		
		this.logControl = new MLogControl(serverConfig);
		this.refreshHelp = new RefreshHelp(logControl, serverConfig.isDebugLazyLoad());
		this.debugLazyLoad = new DebugLazyLoad(serverConfig.isDebugLazyLoad());
		
		this.cQueryEngine = new CQueryEngine(serverConfig.getDatabasePlatform(), logControl, binder);

		this.transactionManager = new TransactionManager(clusterManager, serverConfig, beanDescriptorManager);
		
		ExternalTransactionManager externalTransactionManager = serverConfig.getExternalTransactionManager();
		if (externalTransactionManager != null){
			
			externalTransactionManager.setTransactionManager(transactionManager);
			this.transactionScopeManager = new ExternalTransactionScopeManager(transactionManager,externalTransactionManager);
			logger.info("Using external Transaction Manager");
		} else {
			this.transactionScopeManager = new DefaultTransactionScopeManager(transactionManager);			
		}
		
	}

	public AutoFetchManager createAutoFetchManager(InternalEbeanServer server){
		return AutoFetchManagerFactory.create(server, serverConfig, resourceManager);
	}


	public RelationalQueryEngine createRelationalQueryEngine() {
		return new DefaultRelationalQueryEngine(logControl, binder);
	}
	
	public OrmQueryEngine createOrmQueryEngine(CacheManager serverCache) {
		return new DefaultOrmQueryEngine(beanDescriptorManager,  cQueryEngine, serverCache);
	}
	
	public Persister createPersister(InternalEbeanServer server) {
		return new DefaultPersister(server, serverConfig.isValidateOnSave(), logControl, binder, beanDescriptorManager);
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

	public DeploySqlSelectParser getDeploySqlSelectParser() {
		return deploySqlSelectParser;
	}

	public DeployUtil getDeployUtil() {
		return deployUtil;
	}

	public MLogControl getLogControl() {
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

	
	

}
