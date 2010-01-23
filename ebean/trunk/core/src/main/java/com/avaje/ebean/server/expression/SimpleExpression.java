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
	
	public void addBindValues(SpiExpressionRequest request) {
		
	    ElPropertyValue prop = getElProp(request);
		if (prop != null){
		    if (prop.isAssocOneId()){
	            Object[] ids = prop.getAssocOneIdValues(value);
	            if (ids != null){
	                for (int i = 0; i < ids.length; i++) {
	                    request.addBindValue(ids[i]);
	                }
	            }
	            return;
		    }
		    if (prop.isEncrypted()){
                // bind the key as well as the value
		        String encryptKey = prop.getBeanProperty().getEncryptKey();
		        request.addBindValue(encryptKey);
		    }
		}
		     
		request.addBindValue(value);
	}
	
	public void addSql(SpiExpressionRequest request) {
		ElPropertyValue prop = getElProp(request);
		if (prop != null){
		    if (prop.isAssocOneId()){
	            request.append(prop.getAssocOneIdExpr(propertyName,type.toString()));
	            return;
		    }
		    if (prop.isEncrypted()){
		        String dsql = prop.getBeanProperty().getDecryptSql();
		        request.append(dsql).append(" ").append(type.toString()).append(" ? ");
		        return;
		    }
		}
        request.append(propertyName).append(" ").append(type.toString()).append(" ? ");
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
