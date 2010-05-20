package com.avaje.ebeaninternal.server.expression;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.lucene.LLuceneTypes;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;
import com.avaje.ebeaninternal.server.type.ScalarType;


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
	
	public SimpleExpression(FilterExprPath pathPrefix, String propertyName, Op type, Object value) {
		super(pathPrefix, propertyName);
		this.type = type;
		this.value = value;
	}
	
    public boolean isLuceneResolvable(LuceneResolvableRequest req) {
        
        String propertyName = getPropertyName();
        
        if (!req.indexContains(propertyName)) {
            return false;
        }
        
        ElPropertyValue prop = req.getBeanDescriptor().getElGetValue(propertyName);
        if (prop == null){
            return false;
        } 
        BeanProperty beanProperty = prop.getBeanProperty();
        ScalarType<?> scalarType = beanProperty.getScalarType();
        if (scalarType == null){
            // some complex Associated One type
            return false;
        }
        int luceneType = scalarType.getLuceneType();
        if (LLuceneTypes.BINARY == luceneType){
            return false;
        }
        if (LLuceneTypes.STRING == luceneType){
            if (Op.EQ.equals(type) || Op.NOT_EQ.equals(type)){
                return true;
            }
            return false;
        }
        if (Op.NOT_EQ.equals(type)) {
            return false;    
        }
        return true;
    }
    
    public Query addLuceneQuery(SpiExpressionRequest request) throws ParseException{

        String propertyName = getPropertyName();
        
        ElPropertyValue prop = getElProp(request);
        if (prop == null){
            throw new RuntimeException("Property not found? "+propertyName);
        } 
        BeanProperty beanProperty = prop.getBeanProperty();
        ScalarType<?> scalarType = beanProperty.getScalarType();
        
        int luceneType = scalarType.getLuceneType();
        if (LLuceneTypes.STRING == luceneType){
            
            Object lucVal = (String)scalarType.luceneToIndexValue(value);

            if (Op.EQ.equals(type)){
                QueryParser queryParser = request.createQueryParser(propertyName);
                return queryParser.parse(lucVal.toString());
            }
            if (Op.NOT_EQ.equals(type)){
                QueryParser queryParser = request.createQueryParser(propertyName);
                return queryParser.parse("-"+propertyName+"("+lucVal.toString()+")");
            } 
            throw new RuntimeException("String type only supports EQ and NOT_EQ - "+type);
        }
        
        // Must be a number range expression
        LLuceneRangeExpression exp = new LLuceneRangeExpression(type, value, propertyName, luceneType);
        return exp.buildQuery();
    }

//	public String getPropertyName() {
//		return propertyName;
//	}
	
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
	    
	    String propertyName = getPropertyName();
	    
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
		hc = hc * 31 + propName.hashCode();
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
