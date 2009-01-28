package com.avaje.ebean.server.persist.dml;

import java.util.Set;

import com.avaje.ebean.server.deploy.BeanProperty;

public class GenerateDmlRequest {

	private static final String IS_NULL = " is null";

	final StringBuilder sb = new StringBuilder(100);

	final Set<String> includeProps;
	
	int bindCount;
		
	//Object bean;
	
	String prefix;
	String prefix2;
	String suffix;
	
	public GenerateDmlRequest(Set<String> includeProps) {
		this.includeProps = includeProps;
	}
	
	public GenerateDmlRequest append(String s){
		sb.append(s);
		return this;
	}
	
	public boolean isIncluded(BeanProperty prop) {
		return (includeProps == null || includeProps.contains(prop.getName()));
	}
	
//	public void appendColumnIsNull(ImportedProperty prop) {
//		appendRaw(prop.getLocalDbColumn(),IS_NULL);
//	}

	public void appendColumnIsNull(String column) {
		appendRaw(column,IS_NULL);
	}

	public void appendColumn(String column) {
		appendRaw(column,suffix);
		bindCount++;
	}
	
//	public void appendColumn(ImportedProperty prop) {
//		appendRaw(prop.getLocalDbColumn(),suffix);
//		bindCount++;
//	}
	
//	public void appendColumnIsNull(BeanProperty prop) {
//		appendRaw(prop.getDbColumn(),IS_NULL);
//	}
//	
//	public void appendColumn(BeanProperty prop) {
//
//		appendRaw(prop.getDbColumn(),suffix);
//		bindCount++;
//	}
	
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

//	public Object getBean() {
//		return bean;
//	}
//
//	public void setBean(Object bean) {
//		this.bean = bean;
//	}

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
	
//	public void setPrefix(String prefix, String prefix2, String suffix) {
//		this.prefix = prefix;
//		this.prefix2 = prefix2;
//		this.suffix = suffix;
//	}


}
