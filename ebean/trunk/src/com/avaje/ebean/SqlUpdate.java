package com.avaje.ebean;

public interface SqlUpdate {

	/**
	 * Execute the update returning the number of rows modified.
	 * <p>
	 * After you have executed the SqlUpdate you can bind new variables
	 * using {@link #setParameter(String, Object)} etc and then execute
	 * the SqlUpdate again.
	 * </p>
	 * <p>
	 * For JDBC batch processing refer to {@link Transaction#setBatchMode(boolean)}
	 * and {@link Transaction#setBatchSize(int)}.
	 * </p>
	 * @see com.avaje.ebean.Ebean#execute(SqlUpdate)
	 */
	public abstract int execute();

	/**
	 * Return true if eBean should automatically deduce the table modification
	 * information and process it.
	 * <p>
	 * If this is true then cache invalidation and text index management are
	 * aware of the modification.
	 * </p>
	 */
	public abstract boolean isAutoTableMod();

	/**
	 * Set this to false if you don't want eBean to automatically deduce the
	 * table modification information and process it.
	 * <p>
	 * Set this to false if you don't want any cache invalidation or text index
	 * management to occur. You may do this when say you update only one column
	 * and you know that it is not important for cached objects or text indexes.
	 * </p>
	 */
	public abstract SqlUpdate setAutoTableMod(boolean isAutoTableMod);

	/**
	 * Return the label that can be seen in the transaction logs.
	 */
	public abstract String getLabel();

	/**
	 * Set a descriptive text that can be put into the transaction log.
	 * <p>
	 * Useful when identifying the statement in the transaction log.
	 * </p>
	 */
	public abstract SqlUpdate setLabel(String label);

	/**
	 * Return the sql statement.
	 */
	public abstract String getSql();

	/**
	 * Return the timeout used to execute this statement.
	 */
	public abstract int getTimeout();

	/**
	 * Set the timeout in seconds. Zero implies no limit.
	 * <p>
	 * This will set the query timeout on the underlying PreparedStatement. If
	 * the timeout expires a SQLException will be throw and wrapped in a
	 * PersistenceException.
	 * </p>
	 */
	public abstract SqlUpdate setTimeout(int secs);

	/**
	 * @deprecated Use {@link #setParameter(int, Object)} 
	 */
	public abstract SqlUpdate set(int position, Object value);

	/**
	 * @deprecated Use {@link #setParameter(int, Object)} 
	 */
	public abstract SqlUpdate bind(int position, Object value);

	/**
	 * Set a parameter via its index position.
	 */
	public abstract SqlUpdate setParameter(int position, Object value);

	/**
	 * Set a null parameter via its index position. Exactly the same as
	 * {@link #setNull(int, int)}.
	 */
	public abstract SqlUpdate setNull(int position, int jdbcType);

	/**
	 * @deprecated Use {@link #setNull(int, int)} or
	 *             {@link #setNullParameter(int, int)}
	 */
	public abstract SqlUpdate bindNull(int position, int jdbcType);

	/**
	 * Set a null valued parameter using its index position.
	 */
	public abstract SqlUpdate setNullParameter(int position, int jdbcType);

	/**
	 * @deprecated Use {@link #setParameter(String, Object)} 
	 */
	public abstract SqlUpdate set(String name, Object value);

	/**
	 * @deprecated Use {@link #setParameter(String, Object)} 
	 */
	public abstract SqlUpdate bind(String name, Object value);

	/**
	 * Set a named parameter value.
	 */
	public abstract SqlUpdate setParameter(String name, Object param);

	/**
	 * Set a named parameter that has a null value. Exactly the same as
	 * {@link #setNullParameter(String, int)}.
	 */
	public abstract SqlUpdate setNull(String name, int jdbcType);

	/**
	 * @deprecated Use {@link #setNull(String, int)} or
	 *             {@link #setNullParameter(String, int)}
	 */
	public abstract SqlUpdate bindNull(String name, int jdbcType);

	/**
	 * Set a named parameter that has a null value.
	 */
	public abstract SqlUpdate setNullParameter(String name, int jdbcType);

}