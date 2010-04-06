package com.avaje.ebeaninternal.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;


class CaseInsensitiveEqualExpression extends AbstractExpression {

	private static final long serialVersionUID = -6406036750998971064L;
	
	private final String value;
	
	CaseInsensitiveEqualExpression(String propertyName, String value) {
		super(propertyName);
		this.value = value.toLowerCase();
	}
	
	public void addBindValues(SpiExpressionRequest request) {
        
	    ElPropertyValue prop = getElProp(request);
        if (prop != null && prop.isDbEncrypted()) {
            // bind the key as well as the value
            String encryptKey = prop.getBeanProperty().getEncryptKey().getStringValue();
            request.addBindValue(encryptKey);
        }
          
        request.addBindValue(value);  
	}

	public void addSql(SpiExpressionRequest request) {
		
	    String pname = propertyName;
        
        ElPropertyValue prop = getElProp(request);
        if (prop != null && prop.isDbEncrypted()){
            pname = prop.getBeanProperty().getDecryptProperty(propertyName);
        }
        
		request.append("lower(").append(pname).append(") =? ");
	}

	public int queryAutoFetchHash() {
		int hc = CaseInsensitiveEqualExpression.class.getName().hashCode();
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
