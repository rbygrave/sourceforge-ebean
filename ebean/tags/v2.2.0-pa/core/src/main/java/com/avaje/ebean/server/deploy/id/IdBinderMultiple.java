package com.avaje.ebean.server.deploy.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.PersistenceException;

import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.core.InternString;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.lib.util.MapFromString;

/**
 * Bind an Id that is made up of multiple separate properties.
 * <p>
 * The id passed in for binding is expected to be a map with the key being the
 * String name of the property and the value being that properties bind value.
 * </p>
 */
public final class IdBinderMultiple implements IdBinder {

	private final BeanProperty[] props;

	private final String idProperties;
	
	private final String idInValueSql;
	
	public IdBinderMultiple(BeanProperty[] idProps) {
		this.props = idProps;
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < idProps.length; i++) {
			if (i > 0){
				sb.append(",");
			}
			sb.append(idProps[i].getName());
		}
		idProperties = InternString.intern(sb.toString());
		
		sb = new StringBuilder();
		sb.append("(");
		for (int i = 0; i < props.length; i++) {
			if (i > 0){
				sb.append(",");
			}
			sb.append("?");
		}
		sb.append(")");
		
		idInValueSql = sb.toString();
	}
	
	public void initialise(){
		// do nothing
	}
	
	public int getPropertyCount() {
		return props.length;
	}

	public String getIdProperty() {
		return idProperties;
	}

	
	public BeanProperty findBeanProperty(String dbColumnName) {
		
		for (int i = 0; i < props.length; i++) {
			if (dbColumnName.equalsIgnoreCase(props[i].getDbColumn())){
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
			
			sb.append(props[i].getName());
		}
		
		return sb.toString();
	}
	
	public BeanProperty[] getProperties() {
		return props;
	}
	
	public void addIdInBindValue(SpiExpressionRequest request, Object value) {
		for (int i = 0; i < props.length; i++) {
			request.addBindValue(props[i].getValue(value));
		}
	}

	public void addIdInValueSql(SpiExpressionRequest request) {
		request.append(idInValueSql);
	}

	public String getBindIdInSql(String baseTableAlias) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i = 0; i < props.length; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(baseTableAlias);
			sb.append(".");
			sb.append(props[i].getDbColumn());
		}
		sb.append(")");
		return sb.toString();
	}

	public Object[] getIdValues(Object bean){
		Object[] bindvalues = new Object[props.length];
		for (int i = 0; i < props.length; i++) {
			bindvalues[i] = props[i].getValue(bean);
		}
		return bindvalues;
	}
	
	@SuppressWarnings("unchecked")
	public Object[] getBindValues(Object idValue){
		
		Object[] bindvalues = new Object[props.length];
		// concatenated id as a Map
		try {
			Map<String, ?> uidMap = (Map<String, ?>) idValue;

			for (int i = 0; i < props.length; i++) {
				Object value = uidMap.get(props[i].getName());
				bindvalues[i] = value;
			}

			return bindvalues;
			
		} catch (ClassCastException e) {
			String msg = "Expecting concatinated idValue to be a Map";
			throw new PersistenceException(msg, e);
		}
	}
	
	public Object readSet(DbReadContext ctx, Object bean) throws SQLException {
		
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		boolean notNull = false;
		for (int i = 0; i < props.length; i++) {
			Object value = props[i].readSet(ctx, bean, null);
			if (value != null){
				map.put(props[i].getName(), value);
				notNull = true;
			}
		}
		if (notNull){
			return map;
		} else {
			return null;
		}
	}
	
	public Object read(DbReadContext ctx) throws SQLException {
		
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		boolean notNull = false;
		for (int i = 0; i < props.length; i++) {
			Object value = props[i].read(ctx, 0);
			if (value != null){
				map.put(props[i].getName(), value);
				notNull = true;
			}
		}
		if (notNull){
			return map;
		} else {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public int bindId(PreparedStatement pstmt, int index,
			Object idValue) throws SQLException {

		// concatenated id as a Map
		try {
			Map<String, ?> uidMap = (Map<String, ?>) idValue;

			for (int i = 0; i < props.length; i++) {
				Object value = uidMap.get(props[i].getName());
				props[i].bind(pstmt, ++index, value);
			}
			return index;
		} catch (ClassCastException e) {
			String msg = "Expecting concatinated idValue to be a Map";
			throw new PersistenceException(msg, e);
		}
	}
	
	public void appendSelect(DbSqlContext ctx) {
		for (int i = 0; i < props.length; i++) {
    		props[i].appendSelect(ctx);
		}
	}
	
	public String getAssocOneIdExpr(String prefix, String operator){

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < props.length; i++) {
			if (i > 0) {
				sb.append(" and ");
			}
			if (prefix != null){
				sb.append(prefix);
				sb.append(".");				
			}
			sb.append(props[i].getName());
			sb.append(" ").append(operator);
			sb.append(" ? ");
		}
		return sb.toString();		
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

		// allow Map or String for concatenated id
		Map<?,?> mapVal = null;
		if (idValue instanceof Map<?,?>) {
			mapVal = (Map<?,?>) idValue;
		} else {
			mapVal = MapFromString.parse(idValue.toString());
		}

		// Use a new LinkedHashMap to control the order
		LinkedHashMap<String,Object> newMap = new LinkedHashMap<String, Object>();

		for (int i = 0; i < props.length; i++) {
			BeanProperty prop = props[i];

			Object value = mapVal.get(prop.getName());

			// Convert the property type if required
			value = props[i].getScalarType().toBeanType(value);
			newMap.put(prop.getName(), value);
			if (bean != null) {
				// support PropertyChangeSupport
				prop.setValueIntercept(bean, value);
			}
		}

		return newMap;
	}
}
