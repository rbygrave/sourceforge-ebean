package com.avaje.ebean;

/**
 * Defines the naming convention for converting between logical property names
 * and physical DB column names etc.
 * <p>
 * The main goal of the naming convention is to reduce the amount of
 * configuration required in the mapping (especially when mapping between column
 * and property names).
 * </p>
 * <p>
 * Note that if you do not define a NamingConvention the default one will be used and you can
 * configure it's behaviour via properties.
 * </p>
 */
public interface NamingConvention {

	/**
	 * Returns the table name for a given Class when the @Table annotation has
	 * not set the table name.
	 */
	public String getTableNameFromClass(Class<?> beanClass);

	/**
	 * Return the column name given the property name. 
	 */
	public String getColumnFromProperty(Class<?> beanClass, String beanPropertyName);

	/**
	 * Return the property name from the column name. 
	 * <p>
	 * This is used to help mapping of raw SQL queries onto bean properties.
	 * </p>
	 */
	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName);

	/**
	 * Used to dynamically find a foreign key when more than one exists to
	 * choose from.
	 * <p>
	 * For example say Customer has billingAddress and shippingAddress. The
	 * foreign key column name for billingAddress would be determined as
	 * billing_address_id (where id is the foreignKeySuffix and
	 * toColumnFromProperty() converts camel case to underscore.
	 * </p>
	 */
	public String getForeignKeyColumn(Class<?> beanClass, String propertyName);

	/**
	 * Used by code generator to determine the logical property name for a given
	 * foreign key column.
	 */
	public String getForeignKeyProperty(Class<?> beanClass, String dbForeignKeyColumn);

	/**
	 * Converts DB column names to the property names for MapBeans.
	 * <p>
	 * This defaults to lower casing the DB column name.
	 * </p>
	 */
	public String getMapBeanPropertyFromColumn(String dbColumnName);

	/**
	 * Return the sequence name given the table name.
	 * <p>
	 * Typically you might append "_seq" to the table name as an example. 
	 * </p>
	 */
	public String getSequenceName(String tableName);

	/**
	 * Return the sequence nextVal SQL given the sequence name.
	 * <p>
	 * For Oracle this returns {sequence}.nextval
	 * </p>
	 */
	public String getSequenceNextVal(String sequenceName);

	/**
	 * Used when a DB uses Identity/AutoIncrement columns but does not support
	 * getGeneratedKeys.
	 * <p>
	 * Returns a SQL Select statement that returns the last inserted
	 * Identity/Sequence value.
	 * </p>
	 * <p>
	 * For example MS SQLServer 2000 this would return something like
	 * "select @@IDENTITY".
	 * </p>
	 */
	public String getSelectLastInsertedId(String tableName);

}