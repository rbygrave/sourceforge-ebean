package com.avaje.ebean.server.core;

/**
 * Optimistic concurrency mode used for updates and deletes.
 */
public enum ConcurrencyMode {

	/**
	 * No concurrency checking.
	 */
	NONE,
	
	/**
	 * Use a version column.
	 */
	VERSION,
	
	/**
	 * Use all the columns (except Lobs).
	 */
	ALL
}
