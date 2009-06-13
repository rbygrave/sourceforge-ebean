package com.avaje.ebean.config;


public class MatchingNamingConvention implements NamingConvention {

	public static final String DEFAULT_SEQ_FORMAT = "{table}_seq";
	
	final String sequenceFormat;

	public MatchingNamingConvention(String sequenceFormat) {
		this.sequenceFormat = sequenceFormat;
	}
	
	public MatchingNamingConvention() {
		this(DEFAULT_SEQ_FORMAT);
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

	public String getColumnFromProperty(Class<?> beanClass, String beanPropertyName) {
		return beanPropertyName;
	}

	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName) {
		return dbColumnName;
	}


	public String getSequenceName(String table) {
		
		return sequenceFormat.replace("{table}", table);
	}

	
}
