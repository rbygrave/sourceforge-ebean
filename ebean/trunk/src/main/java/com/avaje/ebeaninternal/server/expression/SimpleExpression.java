package com.avaje.ebeaninternal.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;


class SimpleExpression extends AbstractExpression {

	private static final long serialVersionUID = -382881395755603790L;

	enum Op { 
		EQ(" = ? "),
		NOT_EQ(" <> ? "),
		LT(" < ? "),
		LT_EQ(" <= ? "),
		GT(" > ? "),
		GT_EQ(" >= ? ");
		
		String exp;
		
		Op(String exp){
		    this.exp = exp;
		}
		
		public String bind() {
		    return exp;
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
		    if (prop.isAssocId()){
	            Object[] ids = prop.getAssocOneIdValues(value);
	            if (ids != null){
	                for (int i = 0; i < ids.length; i++) {
	                    request.addBindValue(ids[i]);
	                }
	            }
	            return;
		    }
		    if (prop.isDbEncrypted()){
                // bind the key as well as the value
		        String encryptKey = prop.getBeanProperty().getEncryptKey().getStringValue();
		        request.addBindValue(encryptKey);
		    } else if (prop.isLocalEncrypted()) {
		        // not supporting this for equals (but probably could)
		        // prop.getBeanProperty().getScalarType();
		        
		    }
		}
		     
		request.addBindValue(value);
	}
	
	public void addSql(SpiExpressionRequest request) {
		ElPropertyValue prop = getElProp(request);
		if (prop != null){
		    if (prop.isAssocId()){
	            request.append(prop.getAssocOneIdExpr(propertyName,type.bind()));
	            return;
		    }
		    if (prop.isDbEncrypted()){
		        String dsql = prop.getBeanProperty().getDecryptSql();
		        request.append(dsql).append(type.bind());
		        return;
		    }
		}
        request.append(propertyName).append(type.bind());
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
