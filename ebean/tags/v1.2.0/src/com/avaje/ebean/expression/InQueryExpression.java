package com.avaje.ebean.expression;

import java.util.List;

import com.avaje.ebean.bean.BeanQueryRequest;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.query.CQuery;

/**
 * In expression using a sub query.
 * 
 * @authors Mario and Rob
 */
class InQueryExpression implements Expression {

	private static final long serialVersionUID = 666990277309851644L;

	private final String propertyName;

	private final OrmQuery<?> subQuery;

	private transient CQuery<?> compiledSubQuery;

	public InQueryExpression(String propertyName, OrmQuery<?> subQuery) {
		this.propertyName = propertyName;
		this.subQuery = subQuery;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public int queryAutoFetchHash() {
		int hc = InQueryExpression.class.getName().hashCode();
		hc = hc * 31 + propertyName.hashCode();
		hc = hc * 31 + subQuery.queryAutoFetchHash();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {

		// queryPlanHash executes prior to addSql() or addBindValues()
		// ... so compiledQuery will exist
		compiledSubQuery = compileSubQuery(request);

		int hc = InQueryExpression.class.getName().hashCode();
		hc = hc * 31 + propertyName.hashCode();
		hc = hc * 31 + subQuery.queryPlanHash(request);
		return hc;
	}

	/**
	 * Compile/build the sub query.
	 */
	private CQuery<?> compileSubQuery(BeanQueryRequest<?> queryRequest) {

		InternalEbeanServer ebeanServer = (InternalEbeanServer) queryRequest.getEbeanServer();
		return ebeanServer.compileQuery(subQuery, queryRequest.getTransaction());
	}

	public int queryBindHash() {
		return subQuery.queryBindHash();
	}

	public void addSql(ExpressionRequest request) {

		String subSelect = compiledSubQuery.getGeneratedSql();
		subSelect = subSelect.replace('\n', ' ');
		
		request.append(" (");
		request.append(propertyName);
		request.append(") in (");
		request.append(subSelect);
		request.append(") ");
	}

	public void addBindValues(ExpressionRequest request) {

		List<Object> bindParams = compiledSubQuery.getPredicates().getWhereExprBindValues();

		if (bindParams == null) {
			return;
		}

		for (int i = 0; i < bindParams.size(); i++) {
			request.addBindValue(bindParams.get(i));
		}
	}
}
