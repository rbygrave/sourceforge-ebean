package com.avaje.ebean.server.idgen;

import java.util.UUID;

import com.avaje.ebean.config.dbplatform.IdGenerator;

/**
 * IdGenerator for java util UUID.
 */
public class UuidIdGenerator implements IdGenerator {

    /**
     * The name of the default UUID generator.
     */
    public static final String AUTO_UUID = "auto.uuid";

	/**
	 * Return UUID from UUID.randomUUID();
	 */
	public Object nextId() {
		return UUID.randomUUID();
	}

	
}
