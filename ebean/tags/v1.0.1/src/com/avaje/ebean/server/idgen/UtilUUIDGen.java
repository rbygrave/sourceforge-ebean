package com.avaje.ebean.server.idgen;

import java.util.UUID;

import com.avaje.ebean.server.core.IdGenerator;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * IdGenerator for java util UUID.
 */
public class UtilUUIDGen implements IdGenerator {

	/**
	 * No configuration required.
	 */
	public void configure(String name, InternalEbeanServer server) {
		
	}

	/**
	 * Return UUID from UUID.randomUUID();
	 */
	public Object nextId(BeanDescriptor desc) {
		return UUID.randomUUID();
	}

	
}
