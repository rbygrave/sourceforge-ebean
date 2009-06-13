package com.avaje.ebean.server.core;

import com.avaje.ebean.config.AutofetchConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.DataSourceConfig;


public class ConfigBuilder {

	public ServerConfig build(String serverName) {
		
		ServerConfig config = new ServerConfig();
		config.setName(serverName);	
		config.setAutofetchConfig(new AutofetchConfig());
		config.setDataSourceConfig(new DataSourceConfig());
		
		
		config.loadFromProperties();
		
		return config;
	}
	
	
}
