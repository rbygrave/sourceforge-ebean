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

import java.util.logging.Logger;

import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.cache.ServerCacheManager;
import com.avaje.ebean.config.ExternalTransactionManager;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.ldap.LdapConfig;
import com.avaje.ebean.config.ldap.LdapContextFactory;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonValueAdapter;
import com.avaje.ebeaninternal.api.ClassUtil;
import com.avaje.ebeaninternal.api.SpiBackgroundExecutor;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.autofetch.AutoFetchManager;
import com.avaje.ebeaninternal.server.autofetch.AutoFetchManagerFactory;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptorManager;
import com.avaje.ebeaninternal.server.deploy.DeployOrmXml;
import com.avaje.ebeaninternal.server.deploy.parse.DeployCreateProperties;
import com.avaje.ebeaninternal.server.deploy.parse.DeployInherit;
import com.avaje.ebeaninternal.server.deploy.parse.DeployUtil;
import com.avaje.ebeaninternal.server.expression.DefaultExpressionFactory;
import com.avaje.ebeaninternal.server.jmx.MAdminLogging;
import com.avaje.ebeaninternal.server.lucene.LuceneIndexManager;
import com.avaje.ebeaninternal.server.lucene.NoLuceneIndexManager;
import com.avaje.ebeaninternal.server.persist.Binder;
import com.avaje.ebeaninternal.server.persist.DefaultPersister;
import com.avaje.ebeaninternal.server.query.CQueryEngine;
import com.avaje.ebeaninternal.server.query.DefaultOrmQueryEngine;
import com.avaje.ebeaninternal.server.query.DefaultRelationalQueryEngine;
import com.avaje.ebeaninternal.server.resource.ResourceManager;
import com.avaje.ebeaninternal.server.resource.ResourceManagerFactory;
import com.avaje.ebeaninternal.server.subclass.SubClassManager;
import com.avaje.ebeaninternal.server.text.json.DJsonContext;
import com.avaje.ebeaninternal.server.text.json.DefaultJsonValueAdapter;
import com.avaje.ebeaninternal.server.transaction.DefaultTransactionScopeManager;
import com.avaje.ebeaninternal.server.transaction.ExternalTransactionScopeManager;
import com.avaje.ebeaninternal.server.transaction.JtaTransactionManager;
import com.avaje.ebeaninternal.server.transaction.TransactionManager;
import com.avaje.ebeaninternal.server.transaction.TransactionScopeManager;
import com.avaje.ebeaninternal.server.type.DefaultTypeManager;
import com.avaje.ebeaninternal.server.type.TypeManager;

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
	
	private final DebugLazyLoad debugLazyLoad;
	
	private final TransactionManager transactionManager;
	
	private final TransactionScopeManager transactionScopeManager;
	
	private final CQueryEngine cQueryEngine;
	
	private final ClusterManager clusterManager;
		
	private final ServerCacheManager cacheManager;
	
	private final ExpressionFactory expressionFactory;
	
	private final SpiBackgroundExecutor backgroundExecutor;

	private final PstmtBatch pstmtBatch;
	
	private final XmlConfig xmlConfig;
	
	private final LuceneIndexManager luceneIndexManager;
	
	public InternalConfiguration(XmlConfig xmlConfig, ClusterManager clusterManager, ServerCacheManager cacheManager, 
			SpiBackgroundExecutor backgroundExecutor, ServerConfig serverConfig, 
			BootupClasses bootupClasses, PstmtBatch pstmtBatch) {
		
	    this.xmlConfig = xmlConfig;
		this.pstmtBatch = pstmtBatch;
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
		this.deployUtil = new DeployUtil(typeManager, serverConfig);
		
		this.luceneIndexManager = createLuceneManager(serverConfig);
		
		this.beanDescriptorManager = new BeanDescriptorManager(this, luceneIndexManager);
		beanDescriptorManager.deploy();
		
		this.debugLazyLoad = new DebugLazyLoad(serverConfig.isDebugLazyLoad());
		
		this.transactionManager = new TransactionManager(clusterManager, luceneIndexManager, backgroundExecutor, serverConfig, beanDescriptorManager);

		this.logControl = new MAdminLogging(serverConfig, transactionManager);
		
		this.cQueryEngine = new CQueryEngine(serverConfig.getDatabasePlatform(), 
		        logControl, binder, backgroundExecutor, luceneIndexManager);
		
		ExternalTransactionManager externalTransactionManager = serverConfig.getExternalTransactionManager();
		if (externalTransactionManager == null && serverConfig.isUseJtaTransactionManager()){
		    externalTransactionManager = new JtaTransactionManager();
		}
		if (externalTransactionManager != null){
			externalTransactionManager.setTransactionManager(transactionManager);
			this.transactionScopeManager = new ExternalTransactionScopeManager(transactionManager,externalTransactionManager);
			logger.info("Using Transaction Manager ["+externalTransactionManager.getClass()+"]");
		} else {
			this.transactionScopeManager = new DefaultTransactionScopeManager(transactionManager);			
		}
		
	}

	private LuceneIndexManager createLuceneManager(ServerConfig serverConfig) {
	    
	    if (!DetectLucene.isPresent()){
	        // construct an empty index manager as Lucene is not in the 
	        // class path and we are not expecting any indexes
	        return new NoLuceneIndexManager();
	    }
	    
	    return LuceneManagerFactory.createLuceneManager(clusterManager, backgroundExecutor, serverConfig);
    }
	 
	public JsonContext createJsonContext(SpiEbeanServer server) {
	    
	    String s = serverConfig.getProperty("json.pretty", "false");
	    boolean dfltPretty = "true".equalsIgnoreCase(s);
	    
	    s = serverConfig.getProperty("json.jsonValueAdapter", null);
	    
        JsonValueAdapter va = new DefaultJsonValueAdapter();
	    if (s != null){
	        va = (JsonValueAdapter)ClassUtil.newInstance(s, this.getClass());
	    }
        return new DJsonContext(server, va, dfltPretty);
    }

	public LuceneIndexManager getLuceneIndexManager() {
        return luceneIndexManager;
    }

    public XmlConfig getXmlConfig() {
        return xmlConfig;
    }

    public AutoFetchManager createAutoFetchManager(SpiEbeanServer server){
		return AutoFetchManagerFactory.create(server, serverConfig, resourceManager);
	}


	public RelationalQueryEngine createRelationalQueryEngine() {
		return new DefaultRelationalQueryEngine(logControl, binder, serverConfig.getDatabaseBooleanTrue());
	}
	
	public OrmQueryEngine createOrmQueryEngine() {
		return new DefaultOrmQueryEngine(beanDescriptorManager,  cQueryEngine);
	}
	
	public Persister createPersister(SpiEbeanServer server) {
	    LdapContextFactory ldapCtxFactory = null;
	    LdapConfig ldapConfig = serverConfig.getLdapConfig();
	    if (ldapConfig != null){
	        ldapCtxFactory = ldapConfig.getContextFactory();
	    }
		return new DefaultPersister(server, serverConfig.isValidateOnSave(), binder, beanDescriptorManager, pstmtBatch, ldapCtxFactory);
	}

	public PstmtBatch getPstmtBatch() {
		return pstmtBatch;
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

	public DebugLazyLoad getDebugLazyLoad() {
		return debugLazyLoad;
	}

	public SpiBackgroundExecutor getBackgroundExecutor() {
		return backgroundExecutor;
	}

}
