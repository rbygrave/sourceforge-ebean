package com.avaje.ebean.server.expression;

import com.avaje.ebean.LikeType;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.el.ElPropertyValue;

class LikeExpression extends AbstractExpression {

    private static final long serialVersionUID = -5398151809111172380L;

    private final Object value;

    private final boolean caseInsensitive;

    private final LikeType type;

    LikeExpression(String propertyName, String value, boolean caseInsensitive, LikeType type) {
        super(propertyName);
        this.caseInsensitive = caseInsensitive;
        this.type = type;
        this.value = getValue(value, caseInsensitive, type);
    }

    public void addBindValues(SpiExpressionRequest request) {

        ElPropertyValue prop = getElProp(request);
        if (prop != null && prop.isEncrypted()) {
            // bind the key as well as the value
            String encryptKey = prop.getBeanProperty().getEncryptKey();
            request.addBindValue(encryptKey);
        }
    
         
        request.addBindValue(value);      
    }

    public void addSql(SpiExpressionRequest request) {

        String pname = propertyName;
        
        ElPropertyValue prop = getElProp(request);
        if (prop != null && prop.isEncrypted()){
            pname = prop.getBeanProperty().getDecryptProperty(propertyName);
        }
        if (caseInsensitive) {
            request.append("lower(").append(pname).append(")");
        } else {
            request.append(pname);
        }
        if (type.equals(LikeType.EQUAL_TO)) {
            request.append(" = ? ");
        } else {
            request.append(" like ? ");
        }
    }

    /**
     * Based on caseInsensitive and the property name.
     */
    public int queryAutoFetchHash() {
        int hc = LikeExpression.class.getName().hashCode();
        hc = hc * 31 + (caseInsensitive ? 0 : 1);
        hc = hc * 31 + propertyName.hashCode();
        return hc;
    }

    public int queryPlanHash(BeanQueryRequest<?> request) {
        return queryAutoFetchHash();
    }

    public int queryBindHash() {
        return value.hashCode();
    }

    private static String getValue(String value, boolean caseInsensitive, LikeType type) {
        if (caseInsensitive) {
            value = value.toLowerCase();
        }
        switch (type) {
        case RAW:
            return value;
        case STARTS_WITH:
            return value + "%";
        case ENDS_WITH:
            return "%" + value;
        case CONTAINS:
            return "%" + value + "%";
        case EQUAL_TO:
            return value;

        default:
            throw new RuntimeException("LikeType " + type + " missed?");
        }
    }

}
