package com.avaje.ebean.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.el.ElPropertyValue;


class SimpleExpression extends AbstractExpression {

	private static final long serialVersionUID = -382881395755603790L;

	enum Op { 
		EQ {
			public String toString() {
				return "=";
			}
		},
		NOT_EQ {
			public String toString() {
				return "<>";
			}
		},
		LT {
			public String toString() {
				return "<";
			}
		},
		LT_EQ {
			public String toString() {
				return "<=";
			}
		},
		GT {
			public String toString() {
				return ">";
			}
		},
		GT_EQ {
			public String toString() {
				return ">=";
			}
		}
	}
		
	private final Op type;
	
	private final Object value;
	
	public SimpleExpression(String propertyName, Op type, Object value) {
		super(propertyName);
		this.type = type;
		this.value = value;
	}
	
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * If this is a ManyToOne or OneToOne bean return the ElPropertyValue else
	 * return null.
	 * <p>
	 * This is used to support the "getAssocOneIdValues" scenario where someone
	 * goes query.where().eq("customer", customerBean); instead of
	 * query.where().eq("customer.id", customerBean.getId());
	 * </p>
	 */
	private ElPropertyValue getAssocOneElProp(SpiExpressionRequest request) {
		
		ElPropertyValue elGetValue = request.getBeanDescriptor().getElGetValue(propertyName);
		if (elGetValue != null && elGetValue.isAssocOneId()){
			return elGetValue;
		} else {
			return null;
		}
	}
	
	public void addBindValues(SpiExpressionRequest request) {
		ElPropertyValue assocOne = getAssocOneElProp(request);
		if (assocOne != null){
			Object[] ids = assocOne.getAssocOneIdValues(value);
			if (ids != null){
				for (int i = 0; i < ids.length; i++) {
					request.addBindValue(ids[i]);
				}
			}
		} else {
			request.addBindValue(value);			
		}
	}
	
	public void addSql(SpiExpressionRequest request) {
		ElPropertyValue el = getAssocOneElProp(request);
		if (el != null){
			request.append(el.getAssocOneIdExpr(propertyName,type.toString()));
		} else {
			request.append(propertyName).append(" ").append(type.toString()).append(" ? ");
		}
	}
	
	
	/**
	 * Based on the type and propertyName.
	 */
	public int queryAutoFetchHash() {
		int hc = SimpleExpression.class.getName().hashCode();
		hc = hc * 31 + propertyName.hashCode();
		hc = hc * 31 + type.name().hashCode();
		return hc;
	}
	
	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}

	public int queryBindHash() {
		return value.hashCode();
	}
	
	
}
