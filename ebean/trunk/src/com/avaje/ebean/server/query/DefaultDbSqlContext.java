package com.avaje.ebean.server.query;

import java.util.HashSet;
import java.util.Stack;

import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.deploy.jointree.JoinNode;
import com.avaje.ebean.server.lib.util.StringHelper;

public class DefaultDbSqlContext implements DbSqlContext {

	private static final String NEW_LINE = "\n";
	
	private static final String COMMA = ", ";

	private static final String PERIOD = ".";

	private final String tableAliasPlaceHolder;
	
	private final String columnAliasPrefix;
	
	private final StringBuilder sb = new StringBuilder();

	private final Stack<String> tableAliasStack = new Stack<String>();

	private final Stack<JoinNode> joinStack = new Stack<JoinNode>();

	private int columnIndex;
	 
	private boolean useColumnAlias;
	
	private final boolean alwaysUseColumnAlias;
	
	/**
	 * A Set used to make sure formula joins are only added once to a query.
	 */
	private HashSet<String> formulaJoins;

	/**
	 * Construct for FROM clause (no column alias used).
	 */
	public DefaultDbSqlContext(String tableAliasPlaceHolder) {
		this.tableAliasPlaceHolder = tableAliasPlaceHolder;
		this.columnAliasPrefix = null;
		this.alwaysUseColumnAlias = false;
		this.useColumnAlias = false;
	}
	
	/**
	 * Construct for SELECT clause (with column alias settings).
	 */
	public DefaultDbSqlContext(String tableAliasPlaceHolder, String columnAliasPrefix, boolean alwaysUseColumnAlias) {
		this.tableAliasPlaceHolder = tableAliasPlaceHolder;
		this.columnAliasPrefix = columnAliasPrefix;
		this.alwaysUseColumnAlias = alwaysUseColumnAlias;
		this.useColumnAlias = alwaysUseColumnAlias;
	}
	
	public JoinNode peekJoinNode() {
		return joinStack.peek();
	}

	public void popJoinNode() {
		joinStack.pop();
	}

	public void pushJoinNode(JoinNode node) {
		joinStack.push(node);
	}

	public void pushTableAlias(String tableAlias) {
		tableAliasStack.push(tableAlias);
	}

	public void popTableAlias() {
		tableAliasStack.pop();
	}

	public StringBuilder getBuffer() {
		return sb;
	}

	public DefaultDbSqlContext append(String s) {
		sb.append(s);
		return this;
	}

	public DefaultDbSqlContext append(char s) {
		sb.append(s);
		return this;
	}
	
	/**
	 * Called with withColumnAlias=true when we really need to use a column alias.
	 * <p>
	 * This is for embedded imported keys.
	 * </p>
	 */
	public void setUseColumnAlias(boolean useColumnAlias) {
		if (useColumnAlias){
			this.useColumnAlias = true;
		} else {
			// set it back to the default
			this.useColumnAlias = alwaysUseColumnAlias;
		}
	}

	public void appendFormulaJoin(String sqlFormulaJoin, boolean forceOuterJoin) {

		// replace ${ta} place holder with the real table alias...
		String tableAlias = tableAliasStack.peek();
		String converted = StringHelper.replaceString(sqlFormulaJoin, tableAliasPlaceHolder, tableAlias);

		if (formulaJoins == null){
			formulaJoins = new HashSet<String>();
			
		} else if (formulaJoins.contains(converted)){
			// skip adding a formula join because
			// the same join has already been added.
			return;
		} 
		
		// we only want to add this join once
		formulaJoins.add(converted);
		
		sb.append(NEW_LINE);

		if (forceOuterJoin){	
			if ("join".equals(sqlFormulaJoin.substring(0,4).toLowerCase())){
				// prepend left outer as we are in the 'many' part 
				append(" left outer ");
			}
		}
		
		sb.append(converted);
		sb.append(" ");
	}

	public void appendFormulaSelect(String sqlFormulaSelect) {
		
		String tableAlias = tableAliasStack.peek();
		String converted = StringHelper.replaceString(sqlFormulaSelect, tableAliasPlaceHolder, tableAlias);
		
		sb.append(COMMA);
		sb.append(converted);
	}
	
	public void appendColumn(String column) {
		appendColumn(tableAliasStack.peek(), column);
	}

	public void appendColumn(String tableAlias, String column) {
		sb.append(COMMA);
		sb.append(tableAlias);
		sb.append(PERIOD);
		sb.append(column);

		if (useColumnAlias){
			sb.append(" ");
			sb.append(columnAliasPrefix);
			sb.append(columnIndex);
		}
		columnIndex++;
	}

	public String toString() {
		return sb.toString();
	}

}
