/**
 * 
 */
package com.avaje.ebean.config;

/**
 * The transaction logging levels.
 */
public enum TransactionLogging {
	
	/**
	 * No transaction logging.
	 */
	NONE,
	
	/**
	 * Have transaction logging only for explicit transactions.
	 */
	EXPLICIT,
	
	/**
	 * Have transaction logging for all types of transactions.
	 */
	ALL,
}