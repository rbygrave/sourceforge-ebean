package com.avaje.ebean.server.deploy.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.type.ScalarType;

/**
 * Bind an Id where the Id is made of a single property (not embedded).
 */
public final class IdBinderSimple implements IdBinder {

	final BeanProperty idProperty;
	
	final String bindIdSql;
	
	final BeanProperty[] properties;
	
	final Class<?> expectedType;
	
	final ScalarType scalarType;
	
	public IdBinderSimple(BeanProperty idProperty) {
		this.idProperty = idProperty;
		this.scalarType = idProperty.getScalarType();
		this.expectedType = idProperty.getPropertyType();
		this.properties = new BeanProperty[1];
		properties[0] = idProperty;
		bindIdSql = idProperty.getDbFullName()+" = ? ";
	}

	public void initialise(){
		// do nothing
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

	public String getBindIdSql() {
		return bindIdSql;
	}

	public Object[] getBindValues(Object idValue){
		return new Object[]{idValue};
	}
	
	public int bindId(PreparedStatement pstmt, int index, Object value) throws SQLException {
		value = idProperty.toBeanType(value);
		idProperty.bind(pstmt, ++index, value);
		return index;
	}
	
	public Object readSet(DbReadContext ctx, Object bean) throws SQLException {
		return idProperty.readSet(ctx, bean, null);
	}
	
	public Object read(DbReadContext ctx) throws SQLException {
		return idProperty.read(ctx);
	}
	
	public void appendSelect(DbSqlContext ctx) {
		idProperty.appendSelect(ctx);
	}
	
	public Object convertSetId(Object idValue, Object bean) {
		
		if (!idValue.getClass().equals(expectedType)){
			idValue = scalarType.toBeanType(idValue);		
		}
		
		if (bean != null) {
			idProperty.setValue(bean, idValue);
		}

		return idValue;
	}
}
