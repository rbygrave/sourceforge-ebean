package com.avaje.ebean.server.query;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.avaje.ebean.Expression;
import com.avaje.ebean.el.ElPropertyDeploy;
import com.avaje.ebean.internal.InternalExpression;
import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Controls the modification of a query in preparation for a row Count query.
 */
public class ModifyQuery {

	final BeanDescriptor<?> desc;

	final HashSet<String> expressionIncludes = new HashSet<String>();

	final List<Expression> manyExpressions = new ArrayList<Expression>();

	Set<String> removeJoins;

	boolean includesMany;
	
	public ModifyQuery(BeanDescriptor<?> desc) {
		this.desc = desc;
	}

	public boolean containsMany(InternalExpression expression) {
		String propertyName = expression.getPropertyName();
		ElPropertyDeploy elProp = desc.getElPropertyDeploy(propertyName);
		return elProp.containsMany();		
	}
	
	/**
	 * Return true if this expression should be removed from the where
	 * expression list.
	 */
	public boolean removeMany(InternalExpression expression) {
		String propertyName = expression.getPropertyName();
		ElPropertyDeploy elProp = desc.getElPropertyDeploy(propertyName);
		if (elProp.containsMany()) {
			// not including this expression
			manyExpressions.add(expression);
			return true;

		} else {
			if (elProp.getElPrefix() != null) {
				// joins required to support expressions
				expressionIncludes.add(elProp.getElPrefix());
			}
			return false;
		}
	}

	/**
	 * Return the set of joins that should be removed from the query.
	 */
	public Set<String> removeJoins(Set<String> includes) {

		removeJoins = new HashSet<String>();

		Iterator<String> it = includes.iterator();
		while (it.hasNext()) {
			String joinPropName = it.next();
			if (!expressionIncludes.contains(joinPropName)) {
				removeJoins.add(joinPropName);
			}
		}
		return removeJoins;
	}

	/**
	 * Return the set of joins required to support the expressions.
	 */
	public HashSet<String> getExpressionIncludes() {
		return expressionIncludes;
	}

	/**
	 * Return true if the query includes a join to a many.
	 */
	public boolean isIncludesMany() {
		return includesMany;
	}

}
