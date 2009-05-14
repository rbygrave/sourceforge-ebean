package com.avaje.ebean.server.naming;

/**
 * Where property and DB column names match each other (are the same).
 */
public class MatchingPropertyNaming implements PropertyNamingConvention {

	public String toColumnFromProperty(Class<?> beanClass, String propertyName) {
		return propertyName;
	}

	public String toPropertyFromColumn(Class<?> beanClass, String dbColumn) {
		return dbColumn;
	}

}
