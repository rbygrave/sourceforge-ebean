/**
 * 
 */
package com.avaje.ebean.config;

/**
 * The logging levels available per type of statement.
 */
public enum StatementLogLevel {
	
	/**
	 * No logging.
	 */
	NONE,
	
	/**
	 * Log only a summary level.
	 */
	SUMMARY,
	
	/**
	 * Log summary and additionally the binding variables.
	 */
	BINDING,
	
	/**
	 * Log all including the binding variables and generated SQL/DML.
	 */
	SQL
}