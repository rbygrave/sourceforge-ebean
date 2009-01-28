package com.avaje.ebean.expression;


/**
 * Slightly redundant as Query.setId() ultimately also does the same job.
 */
class IdExpression implements Expression {

	private static final long serialVersionUID = -3065936341718489842L;

	final Object value;
	
	IdExpression(Object value) {
		this.value = value;
	}

	public String getPropertyName() {
		return null;
	}

	
	public void addBindValues(ExpressionRequest request) {
		
		// 'flatten' EmbeddedId and multiple Id cases
		// into an array of the underlying scalar field values
		Object[] bindIdValues = request.getQueryRequest().getBeanDescriptor().getBindIdValues(value);
		for (int i = 0; i < bindIdValues.length; i++) {
			request.addBindValue(bindIdValues[i]);
		}	
	}

	public void addSql(ExpressionRequest request) {
		
		String idSql = request.getQueryRequest().getBeanDescriptor().getBindIdSql();
		
		request.append(idSql).append(" ");
	}

	/**
	 * No properties so this is just a unique static number.
	 */
	public int queryPlanHash() {
		// this number is unique for a given bean type
		// which is all that is required
		return IdExpression.class.hashCode();
	}

	public int queryBindHash() {
		return value.hashCode();
	}
	
	
}
