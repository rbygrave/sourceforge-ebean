package org.avaje.ebean.server.deploy;

import org.avaje.ebean.annotation.NamedUpdate;

/**
 * Deployment information for a named update.
 */
public class DeployNamedUpdate {

	final String name;
	
	final String updateStatement;

	final boolean isSql;
	
	final boolean notifyCache;

	String sqlUpdateStatement;

	public DeployNamedUpdate(NamedUpdate update) {
		this.name = update.name();
		this.updateStatement = update.update();
		this.isSql = update.isSql();
		this.notifyCache = update.notifyCache();
	}

	public void initialise(DeployUpdateParser parser) {
		if (isSql){
			sqlUpdateStatement = updateStatement;
		} else {
			sqlUpdateStatement = parser.parse(updateStatement);
		}
	}
	
	public String getName() {
		return name;
	}

	public String getSqlUpdateStatement() {
		return sqlUpdateStatement;
	}

	public boolean isNotifyCache() {
		return notifyCache;
	}
	
}
