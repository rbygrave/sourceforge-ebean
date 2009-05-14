package com.avaje.ebean.expression;

import java.util.ArrayList;

import com.avaje.ebean.bean.BeanQueryRequest;

/**
 * Junction implementation.
 */
abstract class JunctionExpression implements Junction, Expression {

	private static final long serialVersionUID = -7422204102750462676L;

	static class Conjunction extends JunctionExpression {

		private static final long serialVersionUID = -645619859900030678L;

		Conjunction(){
			super(" and ");
		}
	}
	
	static class Disjunction extends JunctionExpression {
		
		private static final long serialVersionUID = -8464470066692221413L;

		Disjunction(){
			super(" or ");
		}
	}
	
	final ArrayList<Expression> list = new ArrayList<Expression>();

	final String joinType;

	
	JunctionExpression(String joinType) {
		this.joinType = joinType;
	}
	
	public Junction add(Expression item){
		list.add(item);
		return this;
	}
	
	public String getPropertyName() {
		return null;
	}
	
	public void addBindValues(ExpressionRequest request) {
		
		for (int i = 0; i < list.size(); i++) {
			Expression item = list.get(i);
			item.addBindValues(request);
		}
	}
	
	public void addSql(ExpressionRequest request) {
	
		if (!list.isEmpty()){
			request.append("(");
			
			for (int i = 0; i < list.size(); i++) {
				Expression item = list.get(i);
				if (i > 0){
					request.append(joinType);
				}
				item.addSql(request);
			}
			
			request.append(") ");
		}
	}

	/**
	 * Based on Junction type and all the expression contained.
	 */
	public int queryAutoFetchHash() {
		int hc = JunctionExpression.class.getName().hashCode();
		hc = hc * 31 + joinType.hashCode();
		for (int i = 0; i < list.size(); i++) {
			hc = hc * 31 + list.get(i).queryAutoFetchHash();
		}
		
		return hc;
	}
	
	public int queryPlanHash(BeanQueryRequest<?> request) {
		int hc = JunctionExpression.class.getName().hashCode();
		hc = hc * 31 + joinType.hashCode();
		for (int i = 0; i < list.size(); i++) {
			hc = hc * 31 + list.get(i).queryPlanHash(request);
		}
		
		return hc;
	}

	public int queryBindHash() {
		int hc = JunctionExpression.class.getName().hashCode();
		for (int i = 0; i < list.size(); i++) {
			hc = hc * 31 + list.get(i).queryBindHash();
		}
		
		return hc;
	}
	
	
}
