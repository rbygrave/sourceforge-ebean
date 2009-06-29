package com.avaje.ebean.server.deploy.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;

/**
 * Bind an Id that is an Embedded bean.
 */
public final class IdBinderEmbedded implements IdBinder {

	BeanProperty[] props;
		
	BeanDescriptor<?> idDesc;
	
	final BeanPropertyAssocOne<?> embIdProperty;
	
	public IdBinderEmbedded(BeanPropertyAssocOne<?> embIdProperty) {

		this.embIdProperty = embIdProperty;
	}
	
	public void initialise() {
		idDesc = embIdProperty.getTargetDescriptor();
		props = embIdProperty.getProperties();
	}
	
	public BeanProperty findBeanProperty(String dbColumnName){
		for (int i = 0; i < props.length; i++) {
			if (dbColumnName.equalsIgnoreCase(props[i].getDbColumn())) {
				return props[i];
			}
		}
		return null;
	}
	
	public boolean isComplexId(){
		return true;
	}

	public String getDefaultOrderBy() {
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < props.length; i++) {
			if (i > 0){
				sb.append(",");
			}
			
			sb.append(embIdProperty.getName());
			sb.append(".");
			sb.append(props[i].getName());
		}
		
		return sb.toString();
	}

	
	public BeanProperty[] getProperties() {
		return props;
	}
	
	public Object[] getBindValues(Object value){
		
		Object[] bindvalues = new Object[props.length];
		for (int i = 0; i < props.length; i++) {

			BeanProperty idField = (BeanProperty) props[i];
			Object embFieldValue = idField.getValue(value);

			bindvalues[i] = embFieldValue;
		}
		return bindvalues;
	}
	
	public int bindId(PreparedStatement pstmt, int index,
			Object value) throws SQLException {
		
		for (int i = 0; i < props.length; i++) {
			Object embFieldValue = props[i].getValue(value);
			props[i].bind(pstmt, ++index, embFieldValue);
		}
		return index;
	}

	public Object read(DbReadContext ctx) throws SQLException {
		
		Object embId = idDesc.createVanillaBean();
		boolean notNull = false;
		
    	for (int i = 0; i < props.length; i++) {
    		Object value = props[i].readSet(ctx, embId, null);
    		if (value != null){
    			notNull = true;
    		}
		}
    	
    	if (notNull){
    		return embId;
    	} else {
    		return null;
    	}
	}
	
	public Object readSet(DbReadContext ctx, Object bean) throws SQLException {
		
		Object embId = read(ctx);
		if (embId != null){
			embIdProperty.setValue(bean, embId);
			return embId;
		} else {
			return null;
		}
	}
	
	public void appendSelect(DbSqlContext ctx) {
    	for (int i = 0; i < props.length; i++) {
    		props[i].appendSelect(ctx);
		}
	}

	public String getBindIdSql(String baseTableAlias) {
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < props.length; i++) {
			if (i > 0) {
				sb.append(" and ");
			}
			sb.append(baseTableAlias);
			sb.append(".");
			sb.append(props[i].getDbColumn());
			sb.append(" = ? ");
		}
		return sb.toString();
	}
	
	public Object convertSetId(Object idValue, Object bean) {
		
		// can not cast/convert if it is embedded
		if (bean != null){
			// support PropertyChangeSupport
			embIdProperty.setValueIntercept(bean, idValue);
		}
		
		return idValue;
	}
}
