package com.avaje.ebeaninternal.server.expression;

import java.util.ArrayList;

import com.avaje.ebean.Expression;
import com.avaje.ebean.Junction;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

/**
 * Junction implementation.
 */
abstract class JunctionExpression implements Junction, SpiExpression {

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
	
	private final ArrayList<SpiExpression> list = new ArrayList<SpiExpression>();

	private final String joinType;

	
	JunctionExpression(String joinType) {
		this.joinType = joinType;
	}
	
	public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins manyWhereJoin) {
		
		for (int i = 0; i < list.size(); i++) {
			list.get(i).containsMany(desc, manyWhereJoin);
		}
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
			
			for (int i = 0; i < list.size(); i++) {
				SpiExpression item = list.get(i);
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
