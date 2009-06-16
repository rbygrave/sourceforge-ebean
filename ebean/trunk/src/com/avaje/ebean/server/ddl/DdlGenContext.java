package com.avaje.ebean.server.ddl;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.avaje.ebean.config.dbplatform.DbType;
import com.avaje.ebean.config.dbplatform.DbTypeMap;
import com.avaje.ebean.config.dbplatform.DbDdlSyntax;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.type.ScalarType;

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
	final DbDdlSyntax ddlSyntax;
	
	/**
	 * The new line character that is used.
	 */
	final String newLine;
		
	/**
	 * Last content written (used with removeLast())
	 */
	String lastContent;
	
	Set<String> intersectionTables = new HashSet<String>();
	
	List<String> intersectionTablesCreateDdl = new ArrayList<String>();
	List<String> intersectionTablesFkDdl = new ArrayList<String>();
	
	public DdlGenContext(DbTypeMap dbTypeMap, DbDdlSyntax ddlSyntax){
		this.dbTypeMap = dbTypeMap;
		this.ddlSyntax = ddlSyntax;
		this.newLine = ddlSyntax.getNewLine();
	}

	public boolean isProcessIntersectionTable(String tableName){
		
		return intersectionTables.add(tableName);
	}
	
	public void addCreateIntersectionTable(String createTableDdl){
		intersectionTablesCreateDdl.add(createTableDdl);
	}
	
	public void addIntersectionTableFk(String intTableFk){
		intersectionTablesFkDdl.add(intTableFk);
	}
	
	public void addIntersectionCreateTables() {
		for (String intTableCreate : intersectionTablesCreateDdl) {
			stringWriter.write(newLine);
			stringWriter.write(intTableCreate);
		}		
	}

	public void addIntersectionFkeys() {
		stringWriter.write(newLine);
		stringWriter.write(newLine);
		for (String intTableFk : intersectionTablesFkDdl) {
			stringWriter.write(newLine);
			stringWriter.write(intTableFk);
		}
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
	public DbDdlSyntax getDdlSyntax() {
		return ddlSyntax;
	}

	public String getColumnDefn(BeanProperty p) {
		DbType dbType = getDbType(p);
		return p.renderDbType(dbType);
	}
	
	private DbType getDbType(BeanProperty p) {

		ScalarType scalarType = p.getScalarType();
		if (scalarType == null) {
			throw new RuntimeException("No scalarType for " + p.getFullBeanName());
		}
		return dbTypeMap.get(scalarType.getJdbcType());
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
	
	public String pad(String content, int minWidth){
		if (minWidth > 0 && content.length() < minWidth){
			int padding = minWidth - content.length();
			return content + padding(padding);
		}
		return content;
	}
}
