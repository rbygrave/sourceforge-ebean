package com.avaje.ebean.server.naming;

/**
 * Property naming convention.
 */
public interface PropertyNamingConvention {

	/**
	 * Convert the propertyName to a database column name.
	 */
	public String toColumn(String propertyName);

	/**
	 * Convert the column name to a property name.
	 */
	public String toPropertyName(String dbColumn);

}