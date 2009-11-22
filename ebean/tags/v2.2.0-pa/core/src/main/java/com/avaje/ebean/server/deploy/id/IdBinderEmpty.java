package com.avaje.ebean.server.deploy.id;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;

/**
 * For beans with no id properties AKA report type beans.
 */
public final class IdBinderEmpty implements IdBinder {

	private static final String bindIdSql = "";
	
	private static final BeanProperty[] properties = new BeanProperty[0];
	
	public IdBinderEmpty() {

	}
	public void initialise(){
		
	}
		
	public int getPropertyCount() {
		return 0;
	}
	
	public String getIdProperty() {
		return null;
	}

	public BeanProperty findBeanProperty(String dbColumnName) {
		return null;
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

	public String getBindIdSql(String baseTableAlias) {
		return bindIdSql;
	}
	
	public String getAssocOneIdExpr(String prefix, String operator){
		return null;
	}
	
	
	
	public void addIdInBindValue(SpiExpressionRequest request, Object value) {
		
	}

	public void addIdInValueSql(SpiExpressionRequest request) {
	
	}	
	
	public String getBindIdInSql(String baseTableAlias) {
		return null;
	}
	
	public Object[] getIdValues(Object bean){
		return null;
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
