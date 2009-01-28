package com.avaje.ebean.server.deploy;

import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;

import com.avaje.ebean.server.deploy.jointree.JoinTree;

public class DeployNamedQuery {

	final String name;
	
	final String query;
	
	final QueryHint[] hints;
	
	final DeploySqlSelect sqlSelect;
	
	public DeployNamedQuery(NamedQuery namedQuery) {
		this.name = namedQuery.name();
		this.query = namedQuery.query();
		this.hints = namedQuery.hints();
		this.sqlSelect = null;
	}
	
	public DeployNamedQuery(String name, String query, QueryHint[] hints) {
		this(name, query, hints, null);
	}
	
	public DeployNamedQuery(String name, String query, QueryHint[] hints, DeploySqlSelect sqlSelect) {
		this.name = name;
		this.query = query;
		this.hints = hints;
		this.sqlSelect = sqlSelect;
	}

	public void initialise(BeanDescriptor owner, JoinTree joinTree) {
		if (isSqlSelect()){
			sqlSelect.initialise(owner, joinTree);
		}
	}
	
	public boolean isSqlSelect() {
		return sqlSelect != null;
	}
	
	public String getName() {
		return name;
	}

	public String getQuery() {
		return query;
	}

	public QueryHint[] getHints() {
		return hints;
	}
	
	public DeploySqlSelect getSqlSelect() {
		return sqlSelect;
	}
	
}
