package com.avaje.ebean.server.deploy.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.PersistenceException;

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

	final BeanProperty[] idProps;

	public IdBinderMultiple(BeanProperty[] idProps) {
		this.idProps = idProps;
	}
	
	public void initialise(){
		// do nothing
	}
	
	public BeanProperty findBeanProperty(String dbColumnName) {
		
		for (int i = 0; i < idProps.length; i++) {
			if (dbColumnName.equalsIgnoreCase(idProps[i].getDbColumn())){
				return idProps[i];
			}
		}

		return null;
	}
	
	public boolean isComplexId(){
		return true;
	}
	
	public String getDefaultOrderBy() {
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < idProps.length; i++) {
			if (i > 0){
				sb.append(",");
			}
			
			sb.append(idProps[i].getName());
		}
		
		return sb.toString();
	}
	
	public BeanProperty[] getProperties() {
		return idProps;
	}
	
	@SuppressWarnings("unchecked")
	public Object[] getBindValues(Object idValue){
		
		Object[] bindvalues = new Object[idProps.length];
		// concatenated id as a Map
		try {
			Map<String, ?> uidMap = (Map<String, ?>) idValue;

			for (int i = 0; i < idProps.length; i++) {
				Object value = uidMap.get(idProps[i].getName());
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
		for (int i = 0; i < idProps.length; i++) {
			Object value = idProps[i].readSet(ctx, bean, null);
			if (value != null){
				map.put(idProps[i].getName(), value);
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
		for (int i = 0; i < idProps.length; i++) {
			Object value = idProps[i].read(ctx);
			if (value != null){
				map.put(idProps[i].getName(), value);
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

			for (int i = 0; i < idProps.length; i++) {
				Object value = uidMap.get(idProps[i].getName());
				idProps[i].bind(pstmt, ++index, value);
			}
			return index;
		} catch (ClassCastException e) {
			String msg = "Expecting concatinated idValue to be a Map";
			throw new PersistenceException(msg, e);
		}
	}
	
	public void appendSelect(DbSqlContext ctx) {
		for (int i = 0; i < idProps.length; i++) {
    		idProps[i].appendSelect(ctx);
		}
	}
	
	public String getBindIdSql(String baseTableAlias) {
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < idProps.length; i++) {
			if (i > 0) {
				sb.append(" and ");
			}
			sb.append(baseTableAlias);
			sb.append(".");
			sb.append(idProps[i].getDbColumn());
			sb.append(" = ? ");
		}
		return sb.toString();
	}
	
	public Object convertSetId(Object idValue, Object bean) {

		// allow Map or String for concatenated id
		Map<?,?> mapVal = null;
		if (idValue instanceof Map) {
			mapVal = (Map<?,?>) idValue;
		} else {
			mapVal = MapFromString.parse(idValue.toString());
		}

		// Use a new LinkedHashMap to control the order
		LinkedHashMap<String,Object> newMap = new LinkedHashMap<String, Object>();

		for (int i = 0; i < idProps.length; i++) {
			BeanProperty prop = idProps[i];

			Object value = mapVal.get(prop.getName());

			// Convert the property type if required
			value = idProps[i].getScalarType().toBeanType(value);
			newMap.put(prop.getName(), value);
			if (bean != null) {
				// support PropertyChangeSupport
				prop.setValueIntercept(bean, value);
			}
		}

		return newMap;
	}
}
