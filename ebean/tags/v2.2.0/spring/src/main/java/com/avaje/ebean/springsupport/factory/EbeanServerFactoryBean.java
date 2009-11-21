/**
 * Copyright (C) 2009 the original author or authors
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
package com.avaje.ebean.springsupport.factory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.ServerConfig;

/**
 * The Class EbeanServerFactoryBean.
 *
 * @since 18.05.2009
 * @author E Mc Greal
 */
public class EbeanServerFactoryBean implements InitializingBean, FactoryBean {

	/** The server configuration. */
	private ServerConfig serverConfig;

	/** The ebean server. */
	private EbeanServer ebeanServer;

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {

		if (serverConfig == null){
			throw new Exception("No ServerConig set. You must define a ServerConfig bean");
		}

		// Create the new Ebean server using the configuration
		ebeanServer = EbeanServerFactory.create(serverConfig);
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	public Object getObject() throws Exception {
		return ebeanServer;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#getObjectType()
	 */
	public Class<?> getObjectType() {
		return EbeanServer.class;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return true;
	}

	/**
	 * @return the serverConfiguration
	 */
	public ServerConfig getServerConfig() {
		return serverConfig;
	}

	/**
	 * @param serverConfiguration the serverConfiguration to set
	 */
	public void setServerConfig(ServerConfig serverConfiguration) {
		this.serverConfig = serverConfiguration;
	}
}
