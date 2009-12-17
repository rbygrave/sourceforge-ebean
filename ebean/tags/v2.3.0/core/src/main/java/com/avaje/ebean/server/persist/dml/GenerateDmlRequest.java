package com.avaje.ebean.server.persist.dml;

import java.util.Set;

import com.avaje.ebean.server.deploy.BeanProperty;

public class GenerateDmlRequest {

	private static final String IS_NULL = " is null";

	private final boolean emptyStringAsNull;
	
	private final StringBuilder sb = new StringBuilder(100);

	private final Set<String> includeProps;
	
	private final Object oldValues;
	
	private int bindCount;
	
	private String prefix;
	private String prefix2;
	private String suffix;
	
	/**
	 * Create from a PersistRequestBean.
	 */
	public GenerateDmlRequest(boolean emptyStringAsNull, Set<String> includeProps, Object oldValues) {
		this.emptyStringAsNull = emptyStringAsNull;
	    this.includeProps = includeProps;
		this.oldValues = oldValues;
	}

	/**
	 * Create for generating standard all properties DML/SQL.
	 */
	public GenerateDmlRequest(boolean emptyStringAsNull) {
		this(emptyStringAsNull, null,null);
	}
	
	public GenerateDmlRequest append(String s){
		sb.append(s);
		return this;
	}
	
	public boolean isDbNull(Object v){
	    return v == null || (emptyStringAsNull && (v instanceof String) && ((String)v).length() == 0);
	}
	
	public boolean isIncluded(BeanProperty prop) {
		return (includeProps == null || includeProps.contains(prop.getName()));
	}

	public void appendColumnIsNull(String column) {
		appendRaw(column,IS_NULL);
	}

	public void appendColumn(String column) {
		appendRaw(column,suffix);
		bindCount++;
	}
	
	public void appendRaw(String column) {
		appendRaw(column, suffix);
	}
	
	private void appendRaw(String column, String suffik) {
		sb.append(prefix);
		sb.append(column);
		sb.append(suffik);
		
		if (prefix2 != null){
			prefix = prefix2;
			prefix2 = null;
		}
	}
	
	public String toString() {
		return sb.toString();
	}

	public int getBindCount() {
		return bindCount;
	}

	public void setWhereMode() {
		this.prefix = " and ";
		this.prefix2 = " and ";
		this.suffix = "=?";
	}

	public void setWhereIdMode() {
		this.prefix = "";
		this.prefix2 = " and ";
		this.suffix = "=?";
	}


	public void setInsertSetMode() {
		this.prefix = "";
		this.prefix2 = ", ";
		this.suffix = "";
	}
	
	public void setUpdateSetMode() {
		this.prefix = "";
		this.prefix2 = ", ";
		this.suffix = "=?";
	}

	public Object getOldValues() {
		return oldValues;
	}

}
