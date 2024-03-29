package com.avaje.ebeaninternal.server.expression;

import com.avaje.ebean.Expression;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;


final class NotExpression implements SpiExpression, LuceneAwareExpression {

	private static final long serialVersionUID = 5648926732402355781L;

	private static final String NOT = "not (";
	
	private final SpiExpression exp;
	
	NotExpression(Expression exp){
		this.exp = (SpiExpression)exp;
	}
	
	public boolean isLuceneResolvable(LuceneResolvableRequest req) {
        return false;
	    //return exp.isLuceneResolvable(req);
    }
	
    public SpiLuceneExpr createLuceneExpr(SpiExpressionRequest request) {

        return null;
        //FIXME
//        Query innerQuery = exp.addLuceneQuery(request);
//        BooleanQuery q = new BooleanQuery();
//        q.add(innerQuery, Occur.MUST_NOT);
//        return q;
    }

    public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins manyWhereJoin) {
		exp.containsMany(desc, manyWhereJoin);
	}

	public void addBindValues(SpiExpressionRequest request) {
		exp.addBindValues(request);
	}
	
	public void addSql(SpiExpressionRequest request) {
		request.append(NOT);
		exp.addSql(request);
		request.append(") ");
	}

	/**
	 * Based on the expression.
	 */
	public int queryAutoFetchHash() {
		int hc = NotExpression.class.getName().hashCode();
		hc = hc * 31 + exp.queryAutoFetchHash();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		int hc = NotExpression.class.getName().hashCode();
		hc = hc * 31 + exp.queryPlanHash(request);
		return hc;
	}
	
	public int queryBindHash() {
		return exp.queryBindHash();
	}
	
}
