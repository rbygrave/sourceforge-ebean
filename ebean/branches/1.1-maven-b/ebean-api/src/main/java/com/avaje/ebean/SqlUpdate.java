/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebean;

import java.io.Serializable;

import com.avaje.ebean.util.BindParams;

/**
 * A SQL Update Delete or Insert statement that can be executed. For the times
 * when you want to use Sql DML rather than a ORM bean approach. Refer to the
 * Ebean execute() method.
 * <p>
 * Note that SqlUpdate is designed for general DML sql and CallableSql is
 * designed for use with stored procedures.
 * </p>
 * 
 * <pre class="code">
 * // String sql = &quot;update f_topic set post_count = :count where id = :topicId&quot;;
 * 
 * SqlUpdate update = new SqlUpdate();
 * update.setSql(sql);
 * update.setParameter(&quot;count&quot;, 1);
 * update.setParameter(&quot;topicId&quot;, 50);
 * 
 * int modifiedCount = Ebean.execute(update);
 * </pre>
 * 
 * <p>
 * Note that when the SqlUpdate is executed via Ebean.execute() the sql is
 * parsed to determine if it is an update, delete or insert. In addition the
 * table modified is deduced. If <em>isAutoTableMod()</em> is true, then this
 * is then added to the TransactionEvent and cache invalidation etc is
 * maintained. This means you don't need to use the Ebean.externalModification()
 * method as this has already been done.
 * </p>
 * <p>
 * You can sql.setAutoTableMod(false); to stop the automatic table modification
 * </p>
 * 
 * @see com.avaje.ebean.CallableSql
 * @see com.avaje.ebean.Ebean#execute(SqlUpdate)
 */
public final class SqlUpdate implements Serializable {

	static final long serialVersionUID = -6493829438421253102L;

	/**
	 * The parameters used to bind to the sql.
	 */
	BindParams bindParams = new BindParams();

	/**
	 * The sql update or delete statement.
	 */
	String sql;

	/**
	 * Some descriptive text that can be put into the transaction log.
	 */
	String label = "";

	/**
	 * The statement execution timeout.
	 */
	int timeout;

	/**
	 * Automatically detect the table being modified by this sql. This will
	 * register this information so that eBean invalidates cached objects if
	 * required.
	 */
	boolean isAutoTableMod = true;

	transient EbeanServer server;

	/**
	 * Create with a specific server. This means you can use the
	 * SqlUpdate.execute() method.
	 */
	public SqlUpdate(EbeanServer server, String sql) {
		this.server = server;
		this.sql = sql;
	}

	/**
	 * Create with some sql.
	 */
	public SqlUpdate(String updateSql) {
		this.sql = updateSql;
	}

	/**
	 * Create the SqlUpdate.
	 */
	public SqlUpdate() {

	}

	/**
	 * Execute the update returning the number of rows modified.
	 * 
	 * @see com.avaje.ebean.Ebean#execute(SqlUpdate)
	 */
	public int execute() {
		if (server != null) {
			return server.execute(this);
		} else {
			// Hopefully this doesn't catch anyone out...
			return Ebean.execute(this);
		}
	}

	/**
	 * Return true if eBean should automatically deduce the table modification
	 * information and process it.
	 * <p>
	 * If this is true then cache invalidation and text index management are
	 * aware of the modification.
	 * </p>
	 */
	public boolean isAutoTableMod() {
		return isAutoTableMod;
	}

	/**
	 * Set this to false if you don't want eBean to automatically deduce the
	 * table modification information and process it.
	 * <p>
	 * Set this to false if you don't want any cache invalidation or text index
	 * management to occur. You may do this when say you update only one column
	 * and you know that it is not important for cached objects or text indexes.
	 * </p>
	 */
	public SqlUpdate setAutoTableMod(boolean isAutoTableMod) {
		this.isAutoTableMod = isAutoTableMod;
		return this;
	}

	/**
	 * Return the label that can be seen in the transaction logs.
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set a descriptive text that can be put into the transaction log.
	 * <p>
	 * Useful when identifying the statement in the transaction log.
	 * </p>
	 */
	public SqlUpdate setLabel(String label) {
		this.label = label;
		return this;
	}

	/**
	 * Return the sql statement.
	 */
	public String getSql() {
		return sql;
	}

	/**
	 * Return the timeout used to execute this statement.
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Set the timeout in seconds. Zero implies no limit.
	 * <p>
	 * This will set the query timeout on the underlying PreparedStatement. If
	 * the timeout expires a SQLException will be throw and wrapped in a
	 * PersistenceException.
	 * </p>
	 */
	public SqlUpdate setTimeout(int secs) {
		this.timeout = secs;
		return this;
	}

	/**
	 * Set the sql update or delete statement.
	 */
	public SqlUpdate setSql(String updateSql) {
		this.sql = updateSql;
		return this;
	}

	/**
	 * Set a parameter via its index position. The exact same as
	 * {@link #setParameter(int, Object)}.
	 */
	public SqlUpdate set(int position, Object value) {
		bindParams.setParameter(position, value);
		return this;
	}

	/**
	 * @deprecated Use {@link #set(int, Object)} or
	 *             {@link #setParameter(int, Object)}
	 */
	public SqlUpdate bind(int position, Object value) {
		bindParams.setParameter(position, value);
		return this;
	}

	/**
	 * Set a parameter via its index position.
	 */
	public SqlUpdate setParameter(int position, Object value) {
		bindParams.setParameter(position, value);
		return this;
	}

	/**
	 * Set a null parameter via its index position. Exactly the same as
	 * {@link #setNull(int, int)}.
	 */
	public SqlUpdate setNull(int position, int jdbcType) {
		bindParams.setNullParameter(position, jdbcType);
		return this;
	}

	/**
	 * @deprecated Use {@link #setNull(int, int)} or
	 *             {@link #setNullParameter(int, int)}
	 */
	public SqlUpdate bindNull(int position, int jdbcType) {
		bindParams.setNullParameter(position, jdbcType);
		return this;
	}

	/**
	 * Set a null valued parameter using its index position.
	 */
	public SqlUpdate setNullParameter(int position, int jdbcType) {
		bindParams.setNullParameter(position, jdbcType);
		return this;
	}

	/**
	 * Set a named parameter value. Exactly the same as
	 * {@link #setParameter(String, Object)}.
	 */
	public SqlUpdate set(String name, Object value) {
		bindParams.setParameter(name, value);
		return this;
	}

	/**
	 * @deprecated Use {@link #set(String, Object)} or
	 *             {@link #setParameter(String, Object)}
	 */
	public SqlUpdate bind(String name, Object value) {
		bindParams.setParameter(name, value);
		return this;
	}

	/**
	 * Set a named parameter value.
	 */
	public SqlUpdate setParameter(String name, Object param) {
		bindParams.setParameter(name, param);
		return this;
	}

	/**
	 * Set a named parameter that has a null value. Exactly the same as
	 * {@link #setNullParameter(String, int)}.
	 */
	public SqlUpdate setNull(String name, int jdbcType) {
		bindParams.setNullParameter(name, jdbcType);
		return this;
	}

	/**
	 * @deprecated Use {@link #setNull(String, int)} or
	 *             {@link #setNullParameter(String, int)}
	 */
	public SqlUpdate bindNull(String name, int jdbcType) {
		bindParams.setNullParameter(name, jdbcType);
		return this;
	}

	/**
	 * Set a named parameter that has a null value.
	 */
	public SqlUpdate setNullParameter(String name, int jdbcType) {
		bindParams.setNullParameter(name, jdbcType);
		return this;
	}

	/**
	 * Return the bind parameters.
	 */
	BindParams getBindParams() {
		return bindParams;
	}

}
