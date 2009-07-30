package com.avaje.ebean.server.query;

import java.sql.SQLException;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;

public interface SqlTreeNode {

	/**
	 * the new line character used.
	 * <p>
	 * Note that this is removed for logging sql to the transaction log.
	 * </p>
	 */
	public static final char NEW_LINE = '\n';

	public static final String PERIOD = ".";

	public static final String COMMA = ", ";

	/**
	 * Append the required column information to the SELECT part of the sql
	 * statement.
	 */
	public void appendSelect(DbSqlContext ctx);

	/**
	 * Append to the FROM part of the sql.
	 */
	public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin);

	/**
	 * Append any where predicates for inheritance.
	 */
	public void appendWhere(DbSqlContext ctx);

	/**
	 * Load the appropriate information from the SqlSelectReader.
	 * <p>
	 * At a high level this actually controls the reading of the data from the
	 * jdbc resultSet and putting it into the bean etc.
	 * </p>
	 * 
	 */
	public void load(DbReadContext ctx, EntityBean parentBean) throws SQLException;

}
