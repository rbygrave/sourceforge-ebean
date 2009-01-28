package org.avaje.ebean.expression;


class LikeExpression implements Expression {

	private static final long serialVersionUID = -5398151809111172380L;

	enum Type {
		raw, startsWith, endsWith, contains
	};

	final String propertyName;

	final Object value;

	final boolean caseInsensitive;

	LikeExpression(String propertyName, String value, boolean caseInsensitive, Type type) {
		this.propertyName = propertyName;
		this.caseInsensitive = caseInsensitive;
		this.value = getValue(value, caseInsensitive, type);
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void addBindValues(ExpressionRequest request) {
		
		request.addBindValue(value);
	}
	
	public void addSql(ExpressionRequest request) {
		
		if (caseInsensitive) {
			request.append("lower(").append(propertyName).append(")");
		} else {
			request.append(propertyName);
		}
		request.append(" like ? ");
	}

	/**
	 * Based on caseInsensitive and the property name.
	 */
	public int queryPlanHash() {
		int hc = LikeExpression.class.hashCode();
		hc = hc * 31 + (caseInsensitive ? 0 : 1);
		hc = hc * 31 + propertyName.hashCode();
		return hc;
	}
	
	
	
	public int queryBindHash() {
		return value.hashCode();
	}

	private static String getValue(String value, boolean caseInsensitive, Type type){
		if (caseInsensitive){
			value = value.toLowerCase();
		}
		switch (type) {
		case raw:
			return value;
		case startsWith:
			return value+"%";
		case endsWith:
			return "%"+value;
		case contains:
			return "%"+value+"%";

		default:
			throw new RuntimeException("LikeType "+type+" missed?");
		}
	}
	
}
