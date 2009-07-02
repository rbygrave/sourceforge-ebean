package com.avaje.ebean.server.deploy;

import com.avaje.ebean.el.ElPropertyValue;

/**
 * Used to evaluate imported foreign keys so as to avoid unnecssary joins.
 */
public final class BeanFkeyProperty implements ElPropertyValue {

	private final String placeHolder;
	private final String prefix;
	private final String name;
	private final String dbColumn;
	
	public BeanFkeyProperty(String name, String dbColumn) {
		this(null, name, dbColumn);
	}

	public BeanFkeyProperty(String prefix, String name, String dbColumn) {
		this.prefix = prefix;
		this.name = name;
		this.dbColumn = dbColumn;
		this.placeHolder = calcPlaceHolder(prefix, dbColumn);
		
	}
	
	private String calcPlaceHolder(String prefix, String dbColumn){
		if (prefix != null){
			return "${"+prefix+"}"+dbColumn;
		} else {
			return ROOT_ELPREFIX+dbColumn;
		}
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
	
	/**
	 * Returns false.
	 */
	public boolean containsMany(){
		return false;
	}

	public String getDbColumn() {
		return dbColumn;
	}

	public String getName() {
		return name;
	}
	
	public String getElPlaceholder() {
		return placeHolder;
	}

	public String getElPrefix() {
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
