package org.avaje.ebean.server.deploy.parse;

public class MissingTableException extends Exception {

	private static final long serialVersionUID = -1112661440781134849L;

	final String tableName;
	
	public MissingTableException(String msg, String tableName){
		super(msg);
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}
	
}
