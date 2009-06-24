package com.avaje.ebean.server.deploy;

import com.avaje.ebean.el.ElGetValue;
import com.avaje.ebean.el.ElPropertyDeploy;

/**
 * Used to evaluate imported foreign keys so as to avoid unnecssary joins.
 */
public class BeanFkeyProperty implements ElPropertyDeploy, ElGetValue {

	final String prefix;
	final String name;
	final String dbColumn;
	
	public BeanFkeyProperty(String name, String dbColumn) {
		this.prefix = null;
		this.name = name;
		this.dbColumn = dbColumn;
	}

	public BeanFkeyProperty(String prefix, String name, String dbColumn) {
		this.prefix = prefix;
		this.name = name;
		this.dbColumn = dbColumn;
	}
	
	public BeanFkeyProperty create(String expression) {
		int len = expression.length() - name.length()-1;
		String prefix = expression.substring(0,len);
		
		return new BeanFkeyProperty(prefix, name, dbColumn);
	}
	
	/**
	 * Only usable as ElPropertyDeploy.
	 */
	public boolean isDeployOnly() {
		return true;
	}

	public String getDbColumn() {
		return dbColumn;
	}

	public String getName() {
		return name;
	}

	public String getPrefix() {
		return prefix;
	}

	public Object elConvertType(Object value) {
		throw new RuntimeException("ElPropertyDeploy only - not implemented");
	}

	public Object elGetValue(Object bean) {
		throw new RuntimeException("ElPropertyDeploy only - not implemented");
	}

	public BeanProperty getBeanProperty() {
		throw new RuntimeException("ElPropertyDeploy only - not implemented");
	}

	public String getDeployProperty() {
		throw new RuntimeException("ElPropertyDeploy only - not implemented");
	}

	
	
}
