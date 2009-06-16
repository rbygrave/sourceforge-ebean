/**
 * 
 */
package com.avaje.ebean.config;

/**
 * Defines if transactions share a single log file or each have 
 * their own transaction log file.
 */
public enum TransactionLogSharing {
	
	/**
	 * Every transaction has its own log file.
	 */
	NONE,
	
	/**
	 * Explicit transactions each have their own transaction log file
	 * and implicit transaction all share a common log file.
	 */
	EXPLICIT,
	
	/**
	 * All transactions share the same log file.
	 */
	ALL,
}