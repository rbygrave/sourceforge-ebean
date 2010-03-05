package com.avaje.ebeaninternal.server.deploy.id;

import java.sql.SQLException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.core.DefaultSqlUpdate;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.DbReadContext;
import com.avaje.ebeaninternal.server.deploy.DbSqlContext;
import com.avaje.ebeaninternal.server.type.DataBind;

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
		
	public void createLdapNameById(LdapName name, Object id) throws InvalidNameException {        
    }
	
    public void createLdapNameByBean(LdapName name, Object bean) throws InvalidNameException {
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
	
	public void bindId(DefaultSqlUpdate sqlUpdate, Object value) {
        
    }
	
    public void bindId(DataBind dataBind, Object value) throws SQLException {
		
	}
	
    public void loadIgnore(DbReadContext ctx) {
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
