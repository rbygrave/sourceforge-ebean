package com.avaje.ebean.server.naming;

/**
 * Property naming convention.
 */
public interface PropertyNamingConvention {

	/**
	 * Convert the propertyName to a database column name.
	 */
	public String toColumnFromProperty(Class<?> beanClass, String propertyName);

	/**
	 * Convert the column name to a property name.
	 */
	public String toPropertyFromColumn(Class<?> beanClass, String dbColumn);

}