package com.avaje.ebean.config;


/**
 * Naming convention where the database columns and java properties
 * are the same.
 */
public class MatchingNamingConvention extends DefaultNamingConvention {

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
		return beanClass.getSimpleName();
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
