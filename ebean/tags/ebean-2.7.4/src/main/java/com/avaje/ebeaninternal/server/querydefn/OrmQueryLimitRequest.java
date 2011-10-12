package com.avaje.ebeaninternal.server.querydefn;

import com.avaje.ebean.config.dbplatform.SqlLimitRequest;
import com.avaje.ebeaninternal.api.SpiQuery;

public class OrmQueryLimitRequest implements SqlLimitRequest {

	final SpiQuery<?> ormQuery;

	final String sql;
	
	final String sqlOrderBy;
	
	public OrmQueryLimitRequest(String sql, String sqlOrderBy, SpiQuery<?> ormQuery) {
		this.sql = sql;
		this.sqlOrderBy = sqlOrderBy;
		this.ormQuery = ormQuery;
	}
	
	public String getDbOrderBy() {
		return sqlOrderBy;
	}

	public String getDbSql() {
		return sql;
	}

	public int getFirstRow() {
		return ormQuery.getFirstRow();
	}

	public int getMaxRows() {
		return ormQuery.getMaxRows();
	}

	public boolean isDistinct() {
		return ormQuery.isDistinct();
	}

	
}
