package com.avaje.ebean.server.deploy.id;

import java.sql.SQLException;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;

import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.core.DefaultSqlUpdate;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.type.DataBind;

/**
 * Binds id values to prepared statements.
 */
public interface IdBinder {

	/**
	 * Initialise the binder.
	 */
	public void initialise();
	
	/**
     * Adds RDN's to the LdapName using the id value.
     */
    public void createLdapNameById(LdapName name, Object id) throws InvalidNameException;

    /**
     * Adds RDN's to the LdapName using the id value from the bean.
     */
    public void createLdapNameByBean(LdapName name, Object bean) throws InvalidNameException;

	/**
	 * Return the name(s) of the Id property(s).
	 * Comma delimited if there is more than one.
	 * <p>
	 * This can be used to include in a query.
	 * </p>
	 */
	public String getIdProperty();

	/**
	 * Find a BeanProperty that is mapped to the database column.
	 */
	public BeanProperty findBeanProperty(String dbColumnName);
	
	/**
	 * Return the number of scalar properties for this id.
	 */
	public int getPropertyCount();
	
	/**
	 * Return false if the id is a simple scalar and false if it is embedded or concatenated.
	 */
	public boolean isComplexId();
	
	/**
	 * Return the default order by that may need to be used if the query includes a many property.
	 */
	public String getDefaultOrderBy();
	
	/**
	 * Return the values as an array of scalar bindable values.
	 * <p>
	 * For concatenated keys that use an Embedded bean or multiple id properties
	 * this determines the field values are returns them as an Object array.
	 * </p>
	 * <p>
	 * Added primarily for Query.addWhere().add(Expr.idEq()) support.
	 * </p>
	 */
	public Object[] getBindValues(Object idValue);		
	
	/**
	 * Return the id values for a given bean.
	 */
	public Object[] getIdValues(Object bean);
		
	/**
	 * Build a string of the logical expressions.
	 * <p>
	 * Typically used to build a id = ? string.
	 * </p>
	 */
	public String getAssocOneIdExpr(String prefix, String operator);
	
	/**
	 * Binds an id value to a prepared statement.
	 */
	public void bindId(DataBind dataBind, Object value) throws SQLException;

	/**
	 * Bind the id value to a SqlUpdate statement.
	 */
    public void bindId(DefaultSqlUpdate sqlUpdate, Object value);
	
	public void addIdInBindValue(SpiExpressionRequest request, Object value);
	
	/**
	 * Return the sql for binding the id using an IN clause.
	 */
	public String getBindIdInSql(String baseTableAlias);
	
	public void addIdInValueSql(SpiExpressionRequest request);
	
	/**
	 * Read the id value from the result set and set it to the bean also returning it.
	 */
	public Object readSet(DbReadContext ctx, Object bean) throws SQLException;

	public void loadIgnore(DbReadContext ctx);
	
	/**
	 * Read the id value from the result set and return it.
	 */
	public Object read(DbReadContext ctx) throws SQLException;

	public void appendSelect(DbSqlContext ctx);
	
	/**
	 * Return the sql for binding the id to. This includes table alias and columns that make up the id.
	 */
	public String getBindIdSql(String baseTableAlias);
	
	/**
	 * Return the id properties in flat form.
	 */
	public BeanProperty[] getProperties();
	
	/**
	 * Cast or convert the Id value if necessary and optionally set it.
	 * <p>
	 * The Id value is not assumed to be the correct type so it is converted to
	 * the correct type. Typically this is because we could get a Integer, Long
	 * or BigDecimal depending on the JDBC driver and situation.
	 * </p>
	 * <p>
	 * If the bean is not null, then the value is set to the bean.
	 * </p>
	 */
	public Object convertSetId(Object idValue, Object bean);

}
