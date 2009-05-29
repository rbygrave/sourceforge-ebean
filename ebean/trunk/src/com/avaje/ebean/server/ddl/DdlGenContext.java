package com.avaje.ebean.server.ddl;

import java.io.StringWriter;

/**
 * The context used during DDL generation.
 */
public class DdlGenContext {

	final StringWriter stringWriter = new StringWriter();

	/**
	 * Used to map bean types to DB specific types.
	 */
	final DbTypeMap dbTypeMap;
	
	/**
	 * Handles DB specific DDL syntax.
	 */
	final DdlSyntax ddlSyntax;
	
	/**
	 * The new line character that is used.
	 */
	final String newLine;
		
	/**
	 * Last content written (used with removeLast())
	 */
	String lastContent;
	
	public DdlGenContext(DbTypeMap dbTypeMap, DdlSyntax ddlSyntax){
		this.dbTypeMap = dbTypeMap;
		this.ddlSyntax = ddlSyntax;
		this.newLine = ddlSyntax.getNewLine();
	}

	/**
	 * Return the generated content (DDL script).
	 */
	public String getContent(){
		return stringWriter.toString();
	}
	
	/**
	 * Return the map used to determine the DB specific type
	 * for a given bean property.
	 */
	public DbTypeMap getDbTypeMap() {
		return dbTypeMap;
	}
	
	/**
	 * Return object to handle DB specific DDL syntax.
	 */
	public DdlSyntax getDdlSyntax() {
		return ddlSyntax;
	}
	
	/**
	 * Write content to the buffer.
	 */
	public DdlGenContext write(String content, int minWidth){
		
		content = pad(content, minWidth);
		
		if (lastContent != null){
			stringWriter.write(lastContent);
		}
		lastContent = content;
		return this;

	}
	
	/**
	 * Write content to the buffer.
	 */
	public DdlGenContext write(String content){
		return write(content, 0);
	}
	
	public DdlGenContext writeNewLine() {
		flush().write(newLine);
		return this;
	}
	
	/**
	 * Remove the last content that was written.
	 */
	public DdlGenContext removeLast() {
		if (lastContent != null){
			lastContent = null;
		} else {
			throw new RuntimeException("No lastContent to remove?");
		}
		return this;
	}
	
	/**
	 * Flush the content to the buffer.
	 */
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
