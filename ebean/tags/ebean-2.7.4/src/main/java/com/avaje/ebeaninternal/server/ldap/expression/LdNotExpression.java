package com.avaje.ebeaninternal.server.ldap.expression;

import com.avaje.ebean.Expression;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;


final class LdNotExpression implements SpiExpression {

	private static final long serialVersionUID = 5648926732402355782L;

	private static final String NOT = "!";
	
	private final SpiExpression exp;
	
	LdNotExpression(Expression exp){
		this.exp = (SpiExpression)exp;
	}
	
	public boolean isLuceneResolvable(LuceneResolvableRequest req) {
        return exp.isLuceneResolvable(req);
    }

    public SpiLuceneExpr createLuceneExpr(SpiExpressionRequest request) {
        return null;
    }
    
    public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins manyWhereJoin) {
		exp.containsMany(desc, manyWhereJoin);
	}

	public void addBindValues(SpiExpressionRequest request) {
		exp.addBindValues(request);
	}
	
	public void addSql(SpiExpressionRequest request) {
        request.append("(");
		request.append(NOT);
		exp.addSql(request);
		request.append(")");
	}

	/**
	 * Based on the expression.
	 */
	public int queryAutoFetchHash() {
		int hc = LdNotExpression.class.getName().hashCode();
		hc = hc * 31 + exp.queryAutoFetchHash();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		int hc = LdNotExpression.class.getName().hashCode();
		hc = hc * 31 + exp.queryPlanHash(request);
		return hc;
	}
	
	public int queryBindHash() {
		return exp.queryBindHash();
	}
	
}
