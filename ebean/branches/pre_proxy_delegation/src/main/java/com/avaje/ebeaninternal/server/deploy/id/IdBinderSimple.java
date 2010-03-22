package com.avaje.ebeaninternal.server.deploy.id;

import java.sql.SQLException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.core.DefaultSqlUpdate;
import com.avaje.ebeaninternal.server.core.InternString;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.DbReadContext;
import com.avaje.ebeaninternal.server.deploy.DbSqlContext;
import com.avaje.ebeaninternal.server.type.DataBind;
import com.avaje.ebeaninternal.server.type.ScalarType;

/**
 * Bind an Id where the Id is made of a single property (not embedded).
 */
public final class IdBinderSimple implements IdBinder {

	private final BeanProperty idProperty;

	private final String idInLHSSql;

	private final String bindIdSql;
	
	private final BeanProperty[] properties;
	
	private final Class<?> expectedType;
	
	@SuppressWarnings("unchecked")
    private final ScalarType scalarType;
	
	public IdBinderSimple(BeanProperty idProperty) {
		this.idProperty = idProperty;
		this.scalarType = idProperty.getScalarType();
		this.expectedType = idProperty.getPropertyType();
		this.properties = new BeanProperty[1];
		properties[0] = idProperty;
		bindIdSql = InternString.intern(idProperty.getDbColumn()+" = ? ");
		idInLHSSql = idProperty.getDbColumn();
	}
	
	public void initialise(){
		// do nothing
	}
	
	
	public void createLdapNameById(LdapName name, Object id) throws InvalidNameException {
        Rdn rdn = new Rdn(idProperty.getDbColumn(), id);
        name.add(rdn);
    }
	
    public void createLdapNameByBean(LdapName name, Object bean) throws InvalidNameException {
        Object id = idProperty.getValue(bean);
        createLdapNameById(name, id);
    }

    /**
	 * Returns 1.
	 */
	public int getPropertyCount() {
		return 1;
	}

	public String getIdProperty() {
		return idProperty.getName();
	}

	public BeanProperty findBeanProperty(String dbColumnName) {
		if (dbColumnName.equalsIgnoreCase(idProperty.getDbColumn())){
			return idProperty;
		}
		return null;
	}

	public boolean isComplexId(){
		return false;
	}
	
	public String getDefaultOrderBy() {
		return idProperty.getName();
	}
	
	public BeanProperty[] getProperties() {
		return properties;
	}

	public String getBindIdInSql(String baseTableAlias) {
		return baseTableAlias+"."+idProperty.getDbColumn();
	}

	public String getBindIdSql(String baseTableAlias) {
	    if (baseTableAlias == null){
	        return bindIdSql;
	    } else {
	        return baseTableAlias+"."+bindIdSql;	        
	    }
	}

	public Object[] getIdValues(Object bean){
		return new Object[]{idProperty.getValue(bean)};
	}
	
	public Object[] getBindValues(Object idValue){
		return new Object[]{idValue};
	}
	
	public void addIdInLHSSql(SpiExpressionRequest request) {
		request.append(idInLHSSql);
	}

	public void addIdInValueSql(SpiExpressionRequest request) {
		request.append("?");
	}

	public void addIdInBindValue(SpiExpressionRequest request, Object value) {
		request.addBindValue(value);
	}

	public void bindId(DefaultSqlUpdate sqlUpdate, Object value) {
        sqlUpdate.addParameter(value);
    }

    public void bindId(DataBind dataBind, Object value) throws SQLException {
		value = idProperty.toBeanType(value);
		idProperty.bind(dataBind, value);
	}
	
	
	public void loadIgnore(DbReadContext ctx) {
        idProperty.loadIgnore(ctx);
    }

    public Object readSet(DbReadContext ctx, Object bean) throws SQLException {
		Object id = idProperty.read(ctx);
		if (id != null){
		    idProperty.setValue(bean, id);
		}
		return id;
	}
	
	public Object read(DbReadContext ctx) throws SQLException {
		return idProperty.read(ctx);
	}
	
	public void appendSelect(DbSqlContext ctx) {
		idProperty.appendSelect(ctx);
	}
	
	public String getAssocOneIdExpr(String prefix, String operator){

		StringBuilder sb = new StringBuilder();
		if (prefix != null){
			sb.append(prefix);
			sb.append(".");				
		}
		sb.append(idProperty.getName());
		sb.append(" ").append(operator);
		sb.append(" ? ");
		return sb.toString();		
	}

    public String getAssocIdInValueExpr() {
        return "?";
    }
    
    public String getAssocIdInExpr(String prefix) {

        StringBuilder sb = new StringBuilder();
        if (prefix != null) {
            sb.append(prefix);
            sb.append(".");
        }
        sb.append(idProperty.getName());
        return sb.toString();
    }
	
	public Object convertSetId(Object idValue, Object bean) {
		
		if (!idValue.getClass().equals(expectedType)){
			idValue = scalarType.toBeanType(idValue);		
		}
		
		if (bean != null) {
			// support PropertyChangeSupport
			idProperty.setValueIntercept(bean, idValue);
		}

		return idValue;
	}
}
