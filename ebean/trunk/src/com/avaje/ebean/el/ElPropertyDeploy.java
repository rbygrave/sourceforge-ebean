package com.avaje.ebean.el;

/**
 * Used to parse expressions in queries (where, orderBy etc).
 */
public interface ElPropertyDeploy {

	public String getPrefix();

	public String getName();
		
	public String getDbColumn();
}
