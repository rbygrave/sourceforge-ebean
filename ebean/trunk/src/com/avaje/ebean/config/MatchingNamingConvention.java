package com.avaje.ebean.config;

import com.avaje.ebean.config.dbplatform.DatabasePlatform;

/**
 * Naming convention where the database columns and java properties 
 * are the same.
 */
public class MatchingNamingConvention implements NamingConvention {

	public static final String DEFAULT_SEQ_FORMAT = "{table}_seq";
	
	final String sequenceFormat;

	public MatchingNamingConvention(String sequenceFormat) {
		this.sequenceFormat = sequenceFormat;
	}
	
	public MatchingNamingConvention() {
		this(DEFAULT_SEQ_FORMAT);
	}
	
	
	public void setDatabasePlatform(DatabasePlatform databasePlatform) {
		
	}

	/**
	 * Returns the last part of the class name.
	 */
	public String getTableNameFromClass(Class<?> beanClass) {
		
		String clsName = beanClass.getName();
		int dp = clsName.lastIndexOf('.');
		if (dp != -1) {
			clsName = clsName.substring(dp + 1);
		}

		return clsName;
	}

	/**
	 * Returns the bean property name.
	 */
	public String getColumnFromProperty(Class<?> beanClass, String beanPropertyName) {
		return beanPropertyName;
	}

	/**
	 * Returns the database column name.
	 */
	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName) {
		return dbColumnName;
	}

	/**
	 * Return the sequence name given the table name. 
	 */
	public String getSequenceName(String table) {
		
		return sequenceFormat.replace("{table}", table);
	}

	
}
