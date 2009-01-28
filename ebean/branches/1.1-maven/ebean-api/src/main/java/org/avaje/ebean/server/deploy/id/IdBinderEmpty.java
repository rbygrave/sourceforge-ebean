package org.avaje.ebean.server.deploy.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.avaje.ebean.server.deploy.BeanProperty;
import org.avaje.ebean.server.deploy.DbReadContext;
import org.avaje.ebean.server.deploy.DbSqlContext;

/**
 * For beans with no id properties AKA report type beans.
 */
public final class IdBinderEmpty implements IdBinder {

	static final String bindIdSql = "";
	
	static final BeanProperty[] properties = new BeanProperty[0];
	
	static final Object[] bindValues = new Object[0];
	
	
	public IdBinderEmpty() {

	}
	public void initialise(){
		
	}

	public boolean isComplexId(){
		return true;
	}


	public String getDefaultOrderBy() {
		// this should never happen?
		return "";
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
		return index;
	}
	
	public Object readSet(DbReadContext ctx, Object bean) throws SQLException {
		return null;
	}
	
	public Object read(DbReadContext ctx) throws SQLException {
		return null;
	}
	
	public void appendSelect(DbSqlContext ctx) {
	}
	
	public Object convertSetId(Object idValue, Object bean){
		return idValue;
	}
}
