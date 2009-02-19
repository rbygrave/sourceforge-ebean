package com.avaje.ebean.server.deploy;

import com.avaje.ebean.server.deploy.jointree.JoinNode;

/**
 * Used to provide context during sql construction.
 */
public interface DbSqlContext {

//	/**
//	 * The place holder for the current table alias.
//	 * <p>
//	 * Used in SQL Formulas and replaced with the current table alias
//	 * when generating the sql.
//	 * </p>
//	 */
//	public static final String TABLE_ALIAS = "${ta}";
	
	/**
	 * Push the current table alias onto the stack.
	 */
	public void pushTableAlias(String tableAlias);
	
	/**
	 * Pop the current table alias from the stack.
	 */
	public void popTableAlias();

	/**
	 * Append a char directly to the SQL buffer.
	 */
	public DbSqlContext append(char s);
	
	/**
	 * Append a string directly to the SQL buffer.
	 */
	public DbSqlContext append(String s);
	
	/**
	 * Append a column with an explicit table alias.
	 */
	public void appendColumn(String tableAlias, String column);
	
	/**
	 * Append a column with the current table alias.
	 */
	public void appendColumn(String column);
	
	/**
	 * Append a Sql Formula select. This converts the "${ta}" keyword to
	 * the current table alias.
	 */
	public void appendFormulaSelect(String sqlFormulaSelect);
	
	/**
	 * Append a Sql Formula join. This converts the "${ta}" keyword to
	 * the current table alias.
	 */
	public void appendFormulaJoin(String sqlFormulaJoin, boolean forceOuterJoin);

	/**
	 * Return the current join node.
	 */
	public JoinNode peekJoinNode();
	
	/**
	 * Push a join node onto the stack.
	 */
	public void pushJoinNode(JoinNode currentJoinNode);

	/**
	 * Pop a join node off the stack.
	 */
	public void popJoinNode();

}
