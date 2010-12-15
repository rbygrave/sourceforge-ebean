package com.avaje.ebeaninternal.server.ldap.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;

class LdLikeExpression extends LdAbstractExpression {

    private static final long serialVersionUID = 4091359751840929076L;

    private final String value;

    public LdLikeExpression(String propertyName, String value) {
        super(propertyName);
        this.value = value;
    }
    
    public boolean isLuceneResolvable(LuceneResolvableRequest req) {
        return false;
    }
    
    public SpiLuceneExpr createLuceneExpr(SpiExpressionRequest request) {
        return null;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void addBindValues(SpiExpressionRequest request) {
        // Not using binding with wildcards
    }

    public void addSql(SpiExpressionRequest request) {

        String escapedValue;
        if (value == null) {
            escapedValue = "*";
        } else {
            escapedValue = LdEscape.forLike(value);
        } 

        String parsed = request.parseDeploy(propertyName);

        request.append("(").append(parsed).append("=").append(escapedValue).append(")");

    }

    /**
     * Based on the type and propertyName.
     */
    public int queryAutoFetchHash() {
        int hc = LdLikeExpression.class.getName().hashCode();
        hc = hc * 31 + propertyName.hashCode();
        return hc;
    }

    public int queryPlanHash(BeanQueryRequest<?> request) {
        return queryAutoFetchHash();
    }

    public int queryBindHash() {
        return value.hashCode();
    }

}
