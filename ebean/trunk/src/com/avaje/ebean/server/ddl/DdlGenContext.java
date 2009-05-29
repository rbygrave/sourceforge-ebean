package com.avaje.ebean.server.ddl;

import java.io.StringWriter;

public class DdlGenContext {

	final StringWriter stringWriter;

	final DbTypeMap dbTypeMap;
	
	final DdlSyntax ddlSyntax;
	
	final String newLine;
		
	String lastContent;
	
	public DdlGenContext(DbTypeMap dbTypeMap, DdlSyntax ddlSyntax){
		this.dbTypeMap = dbTypeMap;
		this.ddlSyntax = ddlSyntax;
		this.newLine = ddlSyntax.getNewLine();
		this.stringWriter = new StringWriter();
	}

	public String getContent(){
		return stringWriter.toString();
	}
	
	public DbTypeMap getDbTypeMap() {
		return dbTypeMap;
	}
	
	public DdlSyntax getDdlSyntax() {
		return ddlSyntax;
	}
	
	public DdlGenContext write(String content, int minWidth){
		
		content = pad(content, minWidth);
		
		if (lastContent != null){
			stringWriter.write(lastContent);
		}
		lastContent = content;
		return this;

	}
	public DdlGenContext write(String content){
		return write(content, 0);
	}
	
	public DdlGenContext writeNewLine() {
		flush().write(newLine);
		return this;
	}
	
	public DdlGenContext removeLast() {
		if (lastContent != null){
			lastContent = null;
		} else {
			throw new RuntimeException("No lastContent to remove?");
		}
		return this;
	}
	
	public DdlGenContext flush() {
		if (lastContent != null){
			stringWriter.write(lastContent);
		}
		lastContent = null;
		return this;
	}
	
	private String padding(int length){
		
		StringBuffer sb = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			sb.append(" ");
		}
		return sb.toString();
	}
	
	private String pad(String content, int minWidth){
		if (minWidth > 0 && content.length() < minWidth){
			int padding = minWidth - content.length();
			return content + padding(padding);
		}
		return content;
	}
}
