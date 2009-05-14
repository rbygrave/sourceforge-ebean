package com.avaje.ebean.server.deploy.id;

import java.sql.SQLException;

import com.avaje.ebean.MapBean;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;
import com.avaje.ebean.server.persist.dmlbind.BindableRequest;

/**
 * Represents a imported property.
 */
public interface ImportedId {

	/**
	 * Return true if this id is a simple single scalar value. False if it is a
	 * compound id (embedded or multiple).
	 */
	public boolean isScalar();

	/**
	 * Return the logical property name.
	 */
	public String getLogicalName();

	/**
	 * For scalar id return the related single db column.
	 * <p>
	 * This is essentially the imported foreign key column (where there is only
	 * one).
	 * </p>
	 */
	public String getDbColumn();

	/**
	 * Append the the SQL query statement.
	 */
	public void sqlAppend(DbSqlContext ctx);

	/**
	 * Append to the DML statement.
	 */
	public void dmlAppend(GenerateDmlRequest request);

	/**
	 * Append to the DML statement to the where clause.
	 */
	public void dmlWhere(GenerateDmlRequest request, Object bean);

	/**
	 * Return true if the id value has changed.
	 */
	public boolean hasChanged(Object bean, Object oldValues);
	
	/**
	 * Bind the value from the bean.
	 */
	public void bind(BindableRequest request, Object bean, boolean bindNull) throws SQLException;

	/**
	 * For inserting into ManyToMany intersection.
	 */
	public void buildImport(MapBean mapBean, Object other);

	/**
	 * Used to derive a missing concatenated key from multiple imported keys.
	 */
	public BeanProperty findMatchImport(String matchDbColumn);
}