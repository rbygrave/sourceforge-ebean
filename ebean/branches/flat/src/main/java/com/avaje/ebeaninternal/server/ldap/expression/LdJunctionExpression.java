package com.avaje.ebeaninternal.server.ldap.expression;

import java.util.ArrayList;

import com.avaje.ebean.Expression;
import com.avaje.ebean.Junction;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

/**
 * Junction implementation.
 */
abstract class LdJunctionExpression implements Junction, SpiExpression {

	private static final long serialVersionUID = -7422204102750462677L;

	static class Conjunction extends LdJunctionExpression {

		private static final long serialVersionUID = -645619859900030679L;

		Conjunction(){
			super("&");
		}
	}
	
	static class Disjunction extends LdJunctionExpression {
		
		private static final long serialVersionUID = -8464470066692221414L;

		Disjunction(){
			super("|");
		}
	}
	
	private final ArrayList<SpiExpression> list = new ArrayList<SpiExpression>();

	private final String joinType;

	
	LdJunctionExpression(String joinType) {
		this.joinType = joinType;
	}
	
	public boolean containsMany(BeanDescriptor<?> desc) {
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).containsMany(desc)) {
				return true;
			}
		}
		return false;
	}

	public Junction add(Expression item){
		SpiExpression i = (SpiExpression)item;
		list.add(i);
		return this;
	}
	
	public void addBindValues(SpiExpressionRequest request) {
		
		for (int i = 0; i < list.size(); i++) {
			SpiExpression item = list.get(i);
			item.addBindValues(request);
		}
	}
	
	public void addSql(SpiExpressionRequest request) {
	
		if (!list.isEmpty()){
			request.append("(");
            request.append(joinType);
			
			for (int i = 0; i < list.size(); i++) {
				SpiExpression item = list.get(i);
				item.addSql(request);
			}
			
			request.append(") ");
		}
	}

	/**
	 * Based on Junction type and all the expression contained.
	 */
	public int queryAutoFetchHash() {
		int hc = LdJunctionExpression.class.getName().hashCode();
		hc = hc * 31 + joinType.hashCode();
		for (int i = 0; i < list.size(); i++) {
			hc = hc * 31 + list.get(i).queryAutoFetchHash();
		}
		
		return hc;
	}
	
	public int queryPlanHash(BeanQueryRequest<?> request) {
		int hc = LdJunctionExpression.class.getName().hashCode();
		hc = hc * 31 + joinType.hashCode();
		for (int i = 0; i < list.size(); i++) {
			hc = hc * 31 + list.get(i).queryPlanHash(request);
		}
		
		return hc;
	}

	public int queryBindHash() {
		int hc = LdJunctionExpression.class.getName().hashCode();
		for (int i = 0; i < list.size(); i++) {
			hc = hc * 31 + list.get(i).queryBindHash();
		}
		
		return hc;
	}
	
	
}
