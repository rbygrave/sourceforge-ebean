package com.avaje.ebeaninternal.server.ldap.expression;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;


class LdPresentExpression extends LdAbstractExpression {

	
    private static final long serialVersionUID = -4221300142054382003L;

    public LdPresentExpression(String propertyName) {
		super(propertyName);
	}
	
	public boolean isLuceneResolvable(LuceneResolvableRequest req) {
        return false;
    }

    public Query addLuceneQuery(SpiExpressionRequest request) throws ParseException{
        return null;
    }
    
    public String getPropertyName() {
		return propertyName;
	}
	
	public void addBindValues(SpiExpressionRequest request) {
	    // no bind values
	}
	
	public void addSql(SpiExpressionRequest request) {
		
		String parsed = request.parseDeploy(propertyName);
        request.append("(").append(parsed).append("=*").append(")");
	}
	
	
	/**
	 * Based on the type and propertyName.
	 */
	public int queryAutoFetchHash() {
		int hc = LdPresentExpression.class.getName().hashCode();
		hc = hc * 31 + propertyName.hashCode();
		return hc;
	}
	
	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}

	public int queryBindHash() {
		return 1;
	}
	
}
